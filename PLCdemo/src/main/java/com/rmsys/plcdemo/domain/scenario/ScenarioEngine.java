package com.rmsys.plcdemo.domain.scenario;

import com.rmsys.plcdemo.domain.model.ConnectionStatus;
import com.rmsys.plcdemo.domain.model.MachineProfile;
import com.rmsys.plcdemo.domain.model.MachineRuntimeState;
import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.model.MachineState;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ScenarioEngine {

    public void refreshScenario(MachineRuntimeState state, Instant now) {
        if (state.getScenarioUntilTs() != null && now.isAfter(state.getScenarioUntilTs())) {
            state.setCurrentScenario(MachineScenario.NORMAL);
            state.setScenarioUntilTs(null);
            state.setSlowSendIntervalSec(1);
            state.setDuplicatePending(false);
        }

        switch (state.getCurrentScenario()) {
            case WARMUP -> {
                state.setMachineState(MachineState.WARMUP);
                state.setConnectionStatus(ConnectionStatus.ONLINE);
                state.setOperationMode("SETUP");
                state.setSlowSendIntervalSec(1);
            }
            case IDLE -> {
                state.setMachineState(MachineState.IDLE);
                state.setConnectionStatus(ConnectionStatus.ONLINE);
                state.setOperationMode("MANUAL");
                state.setSlowSendIntervalSec(1);
            }
            case OVERHEAT, HIGH_VIBRATION, TOOL_WEAR_RISING -> {
                state.setMachineState(MachineState.RUNNING);
                state.setConnectionStatus(ConnectionStatus.ONLINE);
                state.setOperationMode("AUTO");
                state.setSlowSendIntervalSec(1);
            }
            case EMERGENCY_STOP -> {
                state.setMachineState(MachineState.EMERGENCY_STOP);
                state.setConnectionStatus(ConnectionStatus.ONLINE);
                state.setOperationMode("MANUAL");
                state.setSlowSendIntervalSec(1);
            }
            case MAINTENANCE -> {
                state.setMachineState(MachineState.MAINTENANCE);
                state.setConnectionStatus(ConnectionStatus.ONLINE);
                state.setOperationMode("MANUAL");
                state.setSlowSendIntervalSec(1);
            }
            case DISCONNECT -> {
                state.setMachineState(MachineState.STOPPED);
                state.setConnectionStatus(ConnectionStatus.OFFLINE);
                state.setOperationMode("MANUAL");
                state.setSlowSendIntervalSec(1);
            }
            case SLOWSEND -> {
                state.setMachineState(MachineState.RUNNING);
                state.setConnectionStatus(ConnectionStatus.DEGRADED);
                state.setOperationMode("AUTO");
                if (state.getSlowSendIntervalSec() < 2) {
                    state.setSlowSendIntervalSec(3);
                }
            }
            case DELAYED -> {
                state.setMachineState(MachineState.RUNNING);
                state.setConnectionStatus(ConnectionStatus.DEGRADED);
                state.setOperationMode("AUTO");
                state.setSlowSendIntervalSec(1);
            }
            case DUPLICATE -> {
                state.setMachineState(MachineState.RUNNING);
                state.setConnectionStatus(ConnectionStatus.ONLINE);
                state.setOperationMode("AUTO");
                state.setSlowSendIntervalSec(1);
            }
            case NORMAL -> {
                state.setMachineState(MachineState.RUNNING);
                state.setConnectionStatus(ConnectionStatus.ONLINE);
                state.setOperationMode("AUTO");
                state.setSlowSendIntervalSec(1);
            }
        }
    }

    public ScenarioTargets targets(MachineProfile p, MachineRuntimeState state) {
        double midTemp = (p.getNormalTempMinC() + p.getNormalTempMaxC()) / 2.0;
        double midVib = (p.getNormalVibrationMin() + p.getNormalVibrationMax()) / 2.0;
        boolean machining = supportsMachining(p);
        return switch (state.getCurrentScenario()) {
            case IDLE -> new ScenarioTargets(
                Math.max(28.0, p.getNormalTempMinC() - 8), p.getMinIdlePowerKw(), p.getNormalVibrationMin() * 0.8,
                0, 0, 1.5, 0.05
            );
            case WARMUP -> new ScenarioTargets(
                p.getNormalTempMinC(), p.getNominalRunPowerKw() * 0.55,
                p.getNormalVibrationMin(), machining ? p.getMaxSpindleSpeed() * 0.3 : 0,
                machining ? p.getNormalFeedRate() * 0.45 : 0, 1.6, 0.2
            );
            case OVERHEAT -> new ScenarioTargets(
                p.getDangerTemp() + 4, p.getMaxPowerKw() * 0.94,
                p.getNormalVibrationMax() + 1.5, machining ? p.getMaxSpindleSpeed() * 0.85 : 0,
                machining ? p.getNormalFeedRate() * 0.92 : 0, 1.08, 1.8
            );
            case HIGH_VIBRATION -> new ScenarioTargets(
                p.getNormalTempMaxC() * 1.06, p.getNominalRunPowerKw() * 1.12,
                p.getNormalVibrationMax() * 2.4, machining ? p.getMaxSpindleSpeed() * 0.62 : 0,
                machining ? p.getNormalFeedRate() * 0.78 : 0, 1.2, 2.5
            );
            case TOOL_WEAR_RISING -> new ScenarioTargets(
                p.getNormalTempMaxC(), p.getNominalRunPowerKw() * 1.07,
                p.getNormalVibrationMax() * 1.4, machining ? p.getMaxSpindleSpeed() * 0.55 : 0,
                machining ? p.getNormalFeedRate() : 0, 1.12, 1.9
            );
            case EMERGENCY_STOP -> new ScenarioTargets(
                26.0, p.getMinIdlePowerKw() * 0.75, p.getNormalVibrationMin() * 0.4,
                0, 0, 3.0, 0.0
            );
            case MAINTENANCE -> new ScenarioTargets(
                Math.max(28.0, p.getNormalTempMinC() - 4), p.getMinIdlePowerKw() * 1.1,
                p.getNormalVibrationMin() * 0.5, 0, 0, 2.0, 0.0
            );
            case DISCONNECT -> new ScenarioTargets(
                30.0, p.getMinIdlePowerKw() * 0.7, p.getNormalVibrationMin() * 0.6,
                0, 0, 3.0, 0.0
            );
            case SLOWSEND, DELAYED, DUPLICATE, NORMAL -> new ScenarioTargets(
                midTemp, p.getNominalRunPowerKw(), midVib,
                machining ? p.getMaxSpindleSpeed() * 0.55 : 0,
                machining ? p.getNormalFeedRate() : 0,
                1.0, 1.0
            );
        };
    }

    private boolean supportsMachining(MachineProfile profile) {
        String machineType = profile.getMachineType() == null ? "" : profile.getMachineType().toUpperCase();
        return machineType.contains("CNC") || machineType.contains("MACHIN");
    }

    public record ScenarioTargets(
        double targetTemp,
        double targetPower,
        double targetVibration,
        double spindleTarget,
        double feedTarget,
        double cycleTimeMultiplier,
        double wearMultiplier
    ) {
    }
}
