package com.rmsys.plcdemo.domain.service;

import com.rmsys.plcdemo.config.SimulatorProperties;
import com.rmsys.plcdemo.domain.model.ConnectionStatus;
import com.rmsys.plcdemo.domain.model.MachineProfile;
import com.rmsys.plcdemo.domain.model.MachineRuntimeState;
import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.model.MachineState;
import com.rmsys.plcdemo.domain.model.NormalizedTelemetryDto;
import com.rmsys.plcdemo.domain.scenario.ScenarioEngine;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TelemetryGenerationService {

    private final ScenarioEngine scenarioEngine;
    private final SimulatorProperties properties;

    // Alpha values for emergency stop - much faster drop
    private static final double EMSTOP_ALPHA = 0.55;
    private static final double NORMAL_ALPHA_SPINDLE = 0.18;
    private static final double NORMAL_ALPHA_FEED = 0.18;

    public TelemetryGenerationService(ScenarioEngine scenarioEngine, SimulatorProperties properties) {
        this.scenarioEngine = scenarioEngine;
        this.properties = properties;
    }

    public NormalizedTelemetryDto tick(MachineProfile profile, MachineRuntimeState state, Instant now) {
        ScenarioEngine.ScenarioTargets targets = scenarioEngine.targets(profile, state);
        state.setProgramName(defaultString(profile.getProgramName(), "JOB-" + profile.getMachineCode()));
        state.setToolCode(defaultString(profile.getToolCode(), "TOOL-" + profile.getMachineCode()));

        // Temperature - smooth convergence
        state.setCurrentTemperatureC(
            nextTowards(state.getCurrentTemperatureC(), targets.targetTemp(), 0.10, noise(0.2)));

        // Spindle speed - emergency stop is fast, warmup is slow, normal is smooth
        double spindleAlpha = (state.getCurrentScenario() == MachineScenario.EMERGENCY_STOP) ? EMSTOP_ALPHA
            : (state.getCurrentScenario() == MachineScenario.WARMUP) ? 0.06
            : NORMAL_ALPHA_SPINDLE;
        state.setCurrentSpindleSpeedRpm(
            clamp(nextTowards(state.getCurrentSpindleSpeedRpm(), targets.spindleTarget(), spindleAlpha, noise(20)), 0, profile.getMaxSpindleSpeed()));

        // Feed rate
        double feedAlpha = (state.getCurrentScenario() == MachineScenario.EMERGENCY_STOP) ? EMSTOP_ALPHA : NORMAL_ALPHA_FEED;
        state.setCurrentFeedRateMmMin(
            clamp(nextTowards(state.getCurrentFeedRateMmMin(), targets.feedTarget(), feedAlpha, noise(8)), 0, profile.getNormalFeedRate() * 1.3));

        // Power - derived from spindle + feed + wear
        double spindleRatio = safeRatio(state.getCurrentSpindleSpeedRpm(), profile.getMaxSpindleSpeed());
        double feedRatio = safeRatio(state.getCurrentFeedRateMmMin(), profile.getNormalFeedRate() * 1.2);
        double wearRatio = safeRatio(state.getToolWearPercent(), 100.0);
        double logicalPower = profile.getMinIdlePowerKw() + spindleRatio * 2.3 + feedRatio * 1.8 + wearRatio * 0.6;
        state.setCurrentPowerKw(clamp(
            nextTowards(state.getCurrentPowerKw(), Math.max(logicalPower, targets.targetPower()), 0.18, noise(0.07)),
            0, profile.getMaxPowerKw()));

        // Vibration - derived from spindle + wear + scenario
        double vibBase = profile.getNormalVibrationMin() + spindleRatio * 0.9 + wearRatio * 1.2;
        state.setCurrentVibrationMmS(clamp(
            nextTowards(state.getCurrentVibrationMmS(), Math.max(vibBase, targets.targetVibration()), 0.22, noise(0.04)),
            0.05, profile.getNormalVibrationMax() + 4.0));

        // Cycle / part / wear
        updateCycleAndWear(profile, state, targets);
        updateProcessMetrics(profile, state);

        // Maintenance health
        updateMaintenance(profile, state);

        double tickHours = properties.getTickMs() / 3_600_000.0;
        if (state.getConnectionStatus() != ConnectionStatus.OFFLINE) {
            state.setRuntimeHours(state.getRuntimeHours() + tickHours);
        }
        if (state.getCurrentSpindleSpeedRpm() > 1) {
            state.setServoOnHours(state.getServoOnHours() + tickHours);
        }

        // Energy accumulation
        double energyDelta = state.getCurrentPowerKw() * tickHours;
        state.setEnergyKwhTotal(state.getEnergyKwhTotal() + energyDelta);
        state.setEnergyKwhShift(state.getEnergyKwhShift() + energyDelta);
        state.setEnergyKwhDay(state.getEnergyKwhDay() + energyDelta);
        updateElectricalSignals(profile, state);
        updateThermalBreakdown(profile, state);
        updateLubricationAndBattery(state);
        state.setLastTelemetryTs(now);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("scenario", state.getCurrentScenario().name());
        metadata.put("simulatorNode", properties.getSimulatorNode());
        metadata.put("sendDelayMs", computeSendDelayMs(state));
        metadata.put("source", "SIMULATOR");
        metadata.put("machineCode", profile.getMachineCode());
        metadata.put("cycleCount", state.getCycleCount());
        metadata.put("machineCategory", machineCategory(profile));
        metadata.put("sampleIntervalMs", properties.getTickMs());
        metadata.put("qualityHint", qualityHint(state));
        metadata.put("scenarioPhase", scenarioPhase(state));

        return new NormalizedTelemetryDto(
            profile.getMachineId(),
            profile.getMachineCode(),
            now,
            state.getConnectionStatus(),
            state.getMachineState(),
            state.getOperationMode(),
            state.getProgramName(),
            isCycleRunning(state),
            round2(state.getCurrentPowerKw()),
            round2(state.getCurrentTemperatureC()),
            round2(state.getCurrentVibrationMmS()),
            round2(state.getRuntimeHours()),
            round2(state.getCycleTimeSec()),
            round2(state.getIdealCycleTimeSec()),
            state.getOutputCount(),
            state.getGoodCount(),
            state.getRejectCount(),
            round2(state.getCurrentSpindleSpeedRpm()),
            supportsSpindleLoad(profile) ? round2(state.getSpindleLoadPct()) : null,
            supportsServoLoad(profile) ? round2(state.getServoLoadPct()) : null,
            round2(state.getCurrentFeedRateMmMin()),
            supportsMachining(profile) ? round2(state.getCuttingSpeedMMin()) : null,
            supportsMachining(profile) ? round2(state.getDepthOfCutMm()) : null,
            supportsMachining(profile) ? round2(state.getFeedPerToothMm()) : null,
            supportsMachining(profile) ? round2(state.getWidthOfCutMm()) : null,
            supportsMachining(profile) ? round2(state.getMaterialRemovalRateCm3Min()) : null,
            supportsWeldingCurrent(profile) ? round2(state.getWeldingCurrentA()) : null,
            state.getToolCode(),
            round2(state.getRemainingToolLifePct()),
            round2(state.getVoltageV()),
            round2(state.getCurrentA()),
            round2(state.getPowerFactor()),
            round2(state.getFrequencyHz()),
            round2(state.getEnergyKwhShift()),
            round2(state.getEnergyKwhDay()),
            round2(state.getMotorTemperatureC()),
            round2(state.getBearingTemperatureC()),
            round2(state.getCabinetTemperatureC()),
            round2(state.getServoOnHours()),
            state.getStartStopCount(),
            round2(state.getLubricationLevelPct()),
            state.isBatteryLow(),
            metadata
        );
    }

    private long computeSendDelayMs(MachineRuntimeState state) {
        return switch (state.getCurrentScenario()) {
            case SLOWSEND -> properties.getTickMs() * (Math.max(1, state.getSlowSendIntervalSec()) - 1);
            case DELAYED -> 15_000L;
            default -> 0L;
        };
    }

    private void updateProcessMetrics(MachineProfile profile, MachineRuntimeState state) {
        double spindleRatio = safeRatio(state.getCurrentSpindleSpeedRpm(), profile.getMaxSpindleSpeed());
        double feedRatio = safeRatio(state.getCurrentFeedRateMmMin(), profile.getNormalFeedRate());
        double wearRatio = safeRatio(state.getToolWearPercent(), 100.0);
        double powerRatio = safeRatio(state.getCurrentPowerKw(), profile.getMaxPowerKw());

        double idealBase = Math.max(5.0, profile.getBaseCycleTimeSec() * idealCycleFactor(profile));
        state.setIdealCycleTimeSec(clamp(nextTowards(state.getIdealCycleTimeSec(), idealBase, 0.06, noise(0.03)), 5.0, profile.getBaseCycleTimeSec() * 1.25));

        double spindleLoadTarget = clamp(14 + spindleRatio * 58 + feedRatio * 16 + wearRatio * 18 + powerRatio * 10 + scenarioLoadPenalty(state) * 0.6, 4, 98);
        state.setSpindleLoadPct(clamp(nextTowards(state.getSpindleLoadPct(), spindleLoadTarget, 0.18, noise(0.7)), 0, 100));

        double servoLoadTarget = clamp(10 + feedRatio * 44 + spindleRatio * 24 + scenarioLoadPenalty(state), 3, 96);
        state.setServoLoadPct(clamp(nextTowards(state.getServoLoadPct(), servoLoadTarget, 0.22, noise(0.8)), 0, 100));

        if (supportsMachining(profile)) {
            double toolDiameterMm = profile.getMaxSpindleSpeed() > 7000 ? 8.0 : profile.getMaxSpindleSpeed() > 4500 ? 10.0 : 14.0;
            double cuttingSpeed = Math.PI * toolDiameterMm * state.getCurrentSpindleSpeedRpm() / 1000.0;
            state.setCuttingSpeedMMin(clamp(nextTowards(state.getCuttingSpeedMMin(), cuttingSpeed, 0.25, noise(0.8)), 0, 520));

            double depthTarget = clamp(0.45 + spindleRatio * 2.1 + feedRatio * 1.2 + wearRatio * 0.5, 0.2, 5.0);
            state.setDepthOfCutMm(clamp(nextTowards(state.getDepthOfCutMm(), depthTarget, 0.18, noise(0.05)), 0.15, 6.0));

            double widthTarget = clamp(state.getDepthOfCutMm() * (2.2 + spindleRatio * 0.8), 0.4, 12.0);
            state.setWidthOfCutMm(clamp(nextTowards(state.getWidthOfCutMm(), widthTarget, 0.18, noise(0.08)), 0.35, 14.0));

            double flutes = 4.0;
            double feedPerTooth = state.getCurrentSpindleSpeedRpm() < 1
                ? 0.0
                : state.getCurrentFeedRateMmMin() / (state.getCurrentSpindleSpeedRpm() * flutes);
            state.setFeedPerToothMm(clamp(nextTowards(state.getFeedPerToothMm(), feedPerTooth, 0.20, noise(0.002)), 0.0, 0.38));

            double mrr = (state.getWidthOfCutMm() * state.getDepthOfCutMm() * state.getCurrentFeedRateMmMin()) / 1000.0;
            state.setMaterialRemovalRateCm3Min(clamp(nextTowards(state.getMaterialRemovalRateCm3Min(), mrr, 0.25, noise(0.3)), 0.0, 1800.0));
        } else {
            state.setCuttingSpeedMMin(0.0);
            state.setDepthOfCutMm(0.0);
            state.setFeedPerToothMm(0.0);
            state.setWidthOfCutMm(0.0);
            state.setMaterialRemovalRateCm3Min(0.0);
        }

        if (supportsWeldingCurrent(profile)) {
            double weldingCurrentTarget = clamp(90 + powerRatio * 180 + wearRatio * 18, 60, 320);
            state.setWeldingCurrentA(clamp(nextTowards(state.getWeldingCurrentA(), weldingCurrentTarget, 0.20, noise(1.8)), 50, 340));
        } else {
            state.setWeldingCurrentA(0.0);
        }
    }

    private void updateCycleAndWear(MachineProfile profile, MachineRuntimeState state, ScenarioEngine.ScenarioTargets targets) {
        if (state.getConnectionStatus() == ConnectionStatus.OFFLINE
            || state.getMachineState() == MachineState.EMERGENCY_STOP
            || state.getMachineState() == MachineState.STOPPED
            || state.getMachineState() == MachineState.IDLE
            || state.getMachineState() == MachineState.MAINTENANCE
            || state.getCurrentSpindleSpeedRpm() < 1) {
            return;
        }

        state.setCycleProgressSec(state.getCycleProgressSec() + properties.getTickMs() / 1000.0);
        double cycleTimeSec = Math.max(5, profile.getBaseCycleTimeSec() * targets.cycleTimeMultiplier());
        double minReasonable = Math.max(5, state.getIdealCycleTimeSec() * 0.98);
        state.setCycleTimeSec(Math.max(minReasonable, cycleTimeSec));
        if (state.getCycleProgressSec() >= cycleTimeSec) {
            state.setCycleCount(state.getCycleCount() + 1);
            // WARMUP cycles don't produce parts yet
            if (state.getMachineState() != MachineState.WARMUP) {
                state.setOutputCount(state.getOutputCount() + 1);
                long rejectDelta = rejectDelta(state);
                state.setRejectCount(state.getRejectCount() + rejectDelta);
                // Safety clamp: reject must never exceed output
                if (state.getRejectCount() > state.getOutputCount()) {
                    state.setRejectCount(state.getOutputCount());
                }
                state.setGoodCount(state.getOutputCount() - state.getRejectCount());
            }
            state.setCycleProgressSec(state.getCycleProgressSec() - cycleTimeSec);
            double wearDelta = profile.getToolWearRatePerCycle() * targets.wearMultiplier();
            state.setToolWearPercent(clamp(state.getToolWearPercent() + wearDelta, 0, 100));
        }
    }

    private void updateMaintenance(MachineProfile profile, MachineRuntimeState state) {
        double tempImpact = Math.max(0, (state.getCurrentTemperatureC() - profile.getWarningTemp()) * 0.012);
        double vibImpact = Math.max(0, state.getCurrentVibrationMmS() - profile.getNormalVibrationMax()) * 0.035;
        double wearImpact = state.getToolWearPercent() * 0.0006;
        double decay = profile.getMaintenanceDecayRate() + tempImpact + vibImpact + wearImpact;
        state.setMaintenanceHealthScore(clamp(state.getMaintenanceHealthScore() - decay, 0, 100));
    }

    private void updateElectricalSignals(MachineProfile profile, MachineRuntimeState state) {
        double wearRatio = safeRatio(state.getToolWearPercent(), 100.0);
        double pfPenalty = switch (state.getCurrentScenario()) {
            case OVERHEAT -> 0.05;
            case HIGH_VIBRATION -> 0.04;
            case TOOL_WEAR_RISING -> 0.03;
            default -> 0.0;
        };
        double voltage = nextTowards(state.getVoltageV() <= 0 ? 380.0 : state.getVoltageV(), 380.0, 0.16, noise(1.5));
        double powerFactor = clamp(0.94 - wearRatio * 0.08 - pfPenalty, 0.76, 0.99);
        double currentA = state.getCurrentPowerKw() <= 0.01
            ? 0.0
            : (state.getCurrentPowerKw() * 1_000.0) / (Math.sqrt(3) * Math.max(300.0, voltage) * powerFactor);

        state.setVoltageV(voltage);
        state.setPowerFactor(powerFactor);
        state.setCurrentA(currentA);
        state.setFrequencyHz(clamp(nextTowards(state.getFrequencyHz() <= 0 ? 50.0 : state.getFrequencyHz(), 50.0, 0.12, noise(0.03)), 49.7, 50.3));
        if (state.getProgramName() == null) {
            state.setProgramName(defaultString(profile.getProgramName(), "JOB-" + profile.getMachineCode()));
        }
        if (state.getToolCode() == null) {
            state.setToolCode(defaultString(profile.getToolCode(), "TOOL-" + profile.getMachineCode()));
        }
    }

    private void updateThermalBreakdown(MachineProfile profile, MachineRuntimeState state) {
        double spindleRatio = safeRatio(state.getCurrentSpindleSpeedRpm(), profile.getMaxSpindleSpeed());
        state.setMotorTemperatureC(state.getCurrentTemperatureC() + 2.5 + spindleRatio * 2.0);
        state.setBearingTemperatureC(Math.max(24.0, state.getCurrentTemperatureC() - 3.0 + state.getCurrentVibrationMmS() * 0.7));
        state.setCabinetTemperatureC(Math.max(25.0, 28.0 + state.getCurrentPowerKw() * 1.4));
    }

    private void updateLubricationAndBattery(MachineRuntimeState state) {
        double depletion = state.getRuntimeHours() * 0.015 + state.getCycleCount() * 0.002 + state.getStartStopCount() * 0.03;
        if (state.getCurrentScenario() == MachineScenario.OVERHEAT || state.getCurrentScenario() == MachineScenario.HIGH_VIBRATION) {
            depletion += 0.8;
        }
        state.setLubricationLevelPct(clamp(92.0 - depletion, 8.0, 100.0));
        state.setBatteryLow(state.getMaintenanceHealthScore() < 35.0 || state.getLubricationLevelPct() < 18.0);
    }

    private long rejectDelta(MachineRuntimeState state) {
        double wearRatio = safeRatio(state.getToolWearPercent(), 100.0);
        double chance = switch (state.getCurrentScenario()) {
            case OVERHEAT -> 0.18;
            case HIGH_VIBRATION -> 0.12;
            case TOOL_WEAR_RISING -> 0.10;
            case EMERGENCY_STOP, DISCONNECT, WARMUP, MAINTENANCE, IDLE -> 0.0;
            default -> 0.02 + wearRatio * 0.05;
        };
        return ThreadLocalRandom.current().nextDouble() < chance ? 1 : 0;
    }

    private boolean isCycleRunning(MachineRuntimeState state) {
        return state.getConnectionStatus() != ConnectionStatus.OFFLINE
            && state.getMachineState() == MachineState.RUNNING
            && state.getMachineState() != MachineState.MAINTENANCE
            && (state.getCurrentSpindleSpeedRpm() > 50
            || state.getCurrentFeedRateMmMin() > 5
            || state.getCurrentPowerKw() > 0.5);
    }

    private String qualityHint(MachineRuntimeState state) {
        if (state.getCurrentScenario() == MachineScenario.OVERHEAT || state.getCurrentScenario() == MachineScenario.HIGH_VIBRATION) {
            return "RISK_REJECT_UP";
        }
        if (state.getCurrentScenario() == MachineScenario.TOOL_WEAR_RISING) {
            return "TOOL_WEAR_IMPACT";
        }
        return "STABLE";
    }

    private String scenarioPhase(MachineRuntimeState state) {
        return switch (state.getCurrentScenario()) {
            case WARMUP -> "RAMP_UP";
            case IDLE -> "IDLE";
            case MAINTENANCE -> "MAINTENANCE";
            case OVERHEAT, HIGH_VIBRATION, TOOL_WEAR_RISING -> "FAULT_SIMULATION";
            case EMERGENCY_STOP, DISCONNECT -> "STOPPED";
            case SLOWSEND, DELAYED, DUPLICATE -> "NETWORK_TEST";
            case NORMAL -> "STEADY_RUN";
        };
    }

    private String machineCategory(MachineProfile profile) {
        String machineType = normalizeMachineType(profile);
        if (machineType.contains("ROBOT") && machineType.contains("CNC")) {
            return "ROBOT_CNC_CELL";
        }
        if (machineType.contains("ROBOT")) {
            return "ROBOT_ONLY";
        }
        if (machineType.contains("CNC") || machineType.contains("MACHIN")) {
            return "CNC_MACHINE";
        }
        return machineType;
    }

    private double scenarioLoadPenalty(MachineRuntimeState state) {
        return switch (state.getCurrentScenario()) {
            case OVERHEAT -> 16.0;
            case HIGH_VIBRATION -> 12.0;
            case TOOL_WEAR_RISING -> 10.0;
            case WARMUP -> -8.0;
            case IDLE, EMERGENCY_STOP, DISCONNECT, MAINTENANCE -> -14.0;
            default -> 0.0;
        };
    }

    private boolean supportsMachining(MachineProfile profile) {
        String machineType = normalizeMachineType(profile);
        return machineType.contains("CNC") || machineType.contains("MACHIN");
    }

    private boolean supportsSpindleLoad(MachineProfile profile) {
        return supportsMachining(profile);
    }

    private boolean supportsServoLoad(MachineProfile profile) {
        return !normalizeMachineType(profile).contains("INSPECTION");
    }

    private boolean supportsWeldingCurrent(MachineProfile profile) {
        return normalizeMachineType(profile).contains("WELD");
    }

    private double idealCycleFactor(MachineProfile profile) {
        String machineType = normalizeMachineType(profile);
        if (machineType.contains("PRESS") || machineType.contains("ASSEMBLY") || machineType.contains("INSPECTION")) {
            return 0.96;
        }
        return 0.90;
    }

    private String normalizeMachineType(MachineProfile profile) {
        return defaultString(profile.getMachineType(), "UNKNOWN").toUpperCase();
    }

    private static String defaultString(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static double nextTowards(double current, double target, double alpha, double noise) {
        return current + alpha * (target - current) + noise;
    }

    private static double noise(double maxAbs) {
        return ThreadLocalRandom.current().nextDouble(-maxAbs, maxAbs);
    }

    private static double safeRatio(double numerator, double denominator) {
        if (denominator <= 0) return 0;
        return clamp(numerator / denominator, 0, 1.5);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

