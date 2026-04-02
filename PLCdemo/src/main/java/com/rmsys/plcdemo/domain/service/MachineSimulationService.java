package com.rmsys.plcdemo.domain.service;

import com.rmsys.plcdemo.config.SimulatorProperties;
import com.rmsys.plcdemo.domain.model.ConnectionStatus;
import com.rmsys.plcdemo.domain.model.MachineProfile;
import com.rmsys.plcdemo.domain.model.MachineRuntimeState;
import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.model.MachineState;
import com.rmsys.plcdemo.domain.model.NormalizedTelemetryDto;
import com.rmsys.plcdemo.domain.scenario.ScenarioEngine;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import static com.rmsys.plcdemo.console.ConsoleIo.println;
import static com.rmsys.plcdemo.console.ConsoleIo.printf;

@Service
public class MachineSimulationService {

    private static final Logger log = LoggerFactory.getLogger(MachineSimulationService.class);
    private static final int DASHBOARD_INTERVAL_TICKS = 5;
    private static final int DELAYED_SECONDS = 15;

    private final SimulatorProperties properties;
    private final ScenarioEngine scenarioEngine;
    private final TelemetryGenerationService telemetryGenerationService;
    private final TelemetrySendService telemetrySendService;
    private final Map<String, MachineContext> contexts = new ConcurrentHashMap<>();
    private volatile int dashboardTickCounter = 0;

    public MachineSimulationService(
        SimulatorProperties properties,
        ScenarioEngine scenarioEngine,
        TelemetryGenerationService telemetryGenerationService,
        TelemetrySendService telemetrySendService
    ) {
        this.properties = properties;
        this.scenarioEngine = scenarioEngine;
        this.telemetryGenerationService = telemetryGenerationService;
        this.telemetrySendService = telemetrySendService;
    }

    @PostConstruct
    public void init() {
        if (properties.getMachines().isEmpty()) {
            throw new IllegalStateException("No simulator.machines configured");
        }
        List<String> skippedMachines = new ArrayList<>();
        for (MachineProfile profile : properties.getMachines()) {
            String normalizedCode = normalizeCode(profile.getMachineCode());
            if (!profile.isEnabled()) {
                skippedMachines.add(normalizedCode);
                continue;
            }
            if (normalizedCode == null) {
                throw new IllegalStateException("Machine profile has no machine-code: " + profile.getMachineId());
            }
            if (contexts.putIfAbsent(normalizedCode, new MachineContext(profile, createDefaultState(profile))) != null) {
                throw new IllegalStateException("Duplicate simulator machine-code configured: " + normalizedCode);
            }
        }
        log.info("Simulator initialized with {} machine(s): {}", contexts.size(), contexts.keySet());
        if (!skippedMachines.isEmpty()) {
            log.info("Optional machines disabled by config: {}", skippedMachines);
        }
    }

    public void tickAllMachines() {
        Instant now = Instant.now();
        for (MachineContext ctx : contexts.values()) {
            scenarioEngine.refreshScenario(ctx.state, now);

            // In disconnect mode we do not generate or queue packets.
            if (ctx.state.getCurrentScenario() == MachineScenario.DISCONNECT
                || ctx.state.getConnectionStatus() == ConnectionStatus.OFFLINE) {
                ctx.pendingQueue.clear();
                ctx.delayedBuffer.clear();
                continue;
            }

            // Drain delayed buffer - move expired entries to main send queue
            ctx.drainDelayedToQueue(now);

            NormalizedTelemetryDto dto = telemetryGenerationService.tick(ctx.profile, ctx.state, now);

            // Route snapshot to appropriate queue
            if (ctx.state.getCurrentScenario() == MachineScenario.DELAYED) {
                ctx.addToDelayedBuffer(dto, now.plusSeconds(DELAYED_SECONDS));
            } else {
                ctx.pendingQueue.addLast(dto);
            }

            telemetrySendService.handleSend(ctx.state, ctx.pendingQueue);
        }

        dashboardTickCounter++;
        if (dashboardTickCounter >= DASHBOARD_INTERVAL_TICKS) {
            dashboardTickCounter = 0;
            printDashboard();
        }
    }

    private void printDashboard() {
        println("");
        listStatuses().forEach(row -> {
            printf(
                "[%s] Conn=%s | Sending=%s | Scenario=%s | State=%s | Temp=%.1fC | Power=%.1fkW | Vib=%.2f | Output=%d | ToolLife=%.1f%% | Q=%d/%d | LastSent=%s | LastOk=%s | LastErr=%s%n",
                row.machineCode(),
                row.connectionStatus(),
                row.sending(),
                row.scenario(),
                row.machineState(),
                row.temperatureC(),
                row.powerKw(),
                row.vibrationMmS(),
                row.outputCount(),
                row.remainingToolLifePct(),
                row.pendingQueueSize(),
                row.delayedQueueSize(),
                row.lastSent(),
                row.lastSuccess(),
                row.lastError() == null ? "-" : row.lastError()
            );
        });
    }

    public List<MachineStatusView> listStatuses() {
        List<MachineStatusView> rows = new ArrayList<>();
        for (MachineContext ctx : contexts.values()) {
            rows.add(toStatus(ctx));
        }
        rows.sort(Comparator.comparing(MachineStatusView::machineCode));
        return rows;
    }

    public Optional<MachineStatusView> getMachineStatus(String machineCode) {
        MachineContext ctx = contexts.get(normalizeCode(machineCode));
        if (ctx == null) return Optional.empty();
        return Optional.of(toStatus(ctx));
    }

    public List<String> availableMachineCodes() {
        return contexts.keySet().stream()
            .sorted()
            .collect(Collectors.toList());
    }

    public boolean applyScenario(String machineCode, MachineScenario scenario, Integer durationSec) {
        String normalizedCode = normalizeCode(machineCode);
        MachineContext ctx = contexts.get(normalizedCode);
        if (ctx == null) return false;
        ctx.state.setCurrentScenario(scenario);
        ctx.state.setScenarioUntilTs(durationSec != null && durationSec > 0
            ? Instant.now().plusSeconds(durationSec) : null);
        ctx.state.setDuplicatePending(false);
        if (scenario == MachineScenario.SLOWSEND) {
            ctx.state.setSlowSendIntervalSec(3);
        } else if (scenario == MachineScenario.DISCONNECT) {
            ctx.state.setConnectionStatus(ConnectionStatus.OFFLINE);
            ctx.state.setMachineState(MachineState.STOPPED);
        } else if (scenario == MachineScenario.EMERGENCY_STOP) {
            ctx.state.setMachineState(MachineState.EMERGENCY_STOP);
        } else if (scenario == MachineScenario.MAINTENANCE) {
            ctx.state.setMachineState(MachineState.MAINTENANCE);
        } else {
            ctx.state.setSlowSendIntervalSec(1);
        }
        log.info("Machine {} → scenario {} duration={}s", normalizedCode, scenario, durationSec);
        return true;
    }

    public void applyScenarioAll(MachineScenario scenario, Integer durationSec) {
        contexts.keySet().forEach(code -> applyScenario(code, scenario, durationSec));
    }

    public boolean reconnect(String machineCode) {
        String normalizedCode = normalizeCode(machineCode);
        MachineContext ctx = contexts.get(normalizedCode);
        if (ctx == null) return false;
        ctx.state.setConnectionStatus(ConnectionStatus.ONLINE);
        ctx.state.setMachineState(MachineState.RUNNING);
        ctx.state.setCurrentScenario(MachineScenario.NORMAL);
        ctx.state.setOperationMode("AUTO");
        ctx.state.setScenarioUntilTs(null);
        ctx.state.setSlowSendIntervalSec(1);
        ctx.state.setDuplicatePending(false);
        ctx.state.setStartStopCount(ctx.state.getStartStopCount() + 1);
        log.info("Machine {} reconnected", normalizedCode);
        return true;
    }

    public boolean resetMachine(String machineCode) {
        String normalizedCode = normalizeCode(machineCode);
        MachineContext ctx = contexts.get(normalizedCode);
        if (ctx == null) return false;
        ctx.state = createDefaultState(ctx.profile);
        ctx.pendingQueue.clear();
        ctx.delayedBuffer.clear();
        log.info("Machine {} reset to default state", normalizedCode);
        return true;
    }

    public boolean setSpindleSpeed(String machineCode, double rpm) {
        MachineContext ctx = contexts.get(normalizeCode(machineCode));
        if (ctx == null) return false;
        ctx.state.setCurrentSpindleSpeedRpm(Math.max(0, Math.min(rpm, ctx.profile.getMaxSpindleSpeed())));
        return true;
    }

    public boolean setFeedRate(String machineCode, double mmMin) {
        MachineContext ctx = contexts.get(normalizeCode(machineCode));
        if (ctx == null) return false;
        ctx.state.setCurrentFeedRateMmMin(Math.max(0, mmMin));
        return true;
    }

    public boolean setToolWear(String machineCode, double percent) {
        MachineContext ctx = contexts.get(normalizeCode(machineCode));
        if (ctx == null) return false;
        ctx.state.setToolWearPercent(Math.max(0, Math.min(percent, 100)));
        return true;
    }

    public void pauseAllSend() {
        contexts.values().forEach(ctx -> ctx.state.setSendPaused(true));
    }

    public void resumeAllSend() {
        contexts.values().forEach(ctx -> ctx.state.setSendPaused(false));
    }

    // ---- private helpers ----

    private MachineRuntimeState createDefaultState(MachineProfile profile) {
        MachineRuntimeState state = new MachineRuntimeState();
        state.setConnectionStatus(ConnectionStatus.ONLINE);
        state.setMachineState(MachineState.WARMUP);
        state.setCurrentTemperatureC(profile.getNormalTempMinC());
        state.setCurrentVibrationMmS(profile.getNormalVibrationMin());
        state.setCurrentPowerKw(profile.getMinIdlePowerKw());
        state.setCurrentSpindleSpeedRpm(0);
        state.setCurrentFeedRateMmMin(0);
        state.setCycleCount(0);
        state.setOutputCount(0);
        state.setGoodCount(0);
        state.setRejectCount(0);
        state.setToolWearPercent(2);
        state.setMaintenanceHealthScore(100);
        state.setEnergyKwhTotal(0);
        state.setEnergyKwhShift(0);
        state.setEnergyKwhDay(0);
        state.setRuntimeHours(0);
        state.setServoOnHours(0);
        state.setCycleTimeSec(profile.getBaseCycleTimeSec());
        state.setIdealCycleTimeSec(Math.max(5.0, profile.getBaseCycleTimeSec() * 0.9));
        state.setSpindleLoadPct(0.0);
        state.setServoLoadPct(0.0);
        state.setCuttingSpeedMMin(0.0);
        state.setDepthOfCutMm(0.0);
        state.setFeedPerToothMm(0.0);
        state.setWidthOfCutMm(0.0);
        state.setMaterialRemovalRateCm3Min(0.0);
        state.setWeldingCurrentA(0.0);
        state.setOperationMode("SETUP");
        state.setProgramName(profile.getProgramName());
        state.setToolCode(profile.getToolCode());
        state.setVoltageV(380.0);
        state.setCurrentA(0.0);
        state.setPowerFactor(0.95);
        state.setFrequencyHz(50.0);
        state.setMotorTemperatureC(profile.getNormalTempMinC() + 2.5);
        state.setBearingTemperatureC(Math.max(24.0, profile.getNormalTempMinC() - 3.0));
        state.setCabinetTemperatureC(29.0);
        state.setStartStopCount(1);
        state.setLubricationLevelPct(92.0);
        state.setBatteryLow(false);
        state.setCycleProgressSec(0);
        state.setCurrentScenario(MachineScenario.WARMUP);
        state.setScenarioUntilTs(Instant.now().plusSeconds(30));
        state.setLastTelemetryTs(null);
        state.setLastSentTs(null);
        state.setLastSuccessTs(null);
        state.setLastErrorMessage(null);
        state.setSendPaused(false);
        state.setDuplicatePending(false);
        state.setSlowSendIntervalSec(1);
        state.setTickCounter(0);
        return state;
    }

    private MachineStatusView toStatus(MachineContext ctx) {
        MachineRuntimeState st = ctx.state;
        Instant now = Instant.now();
        return new MachineStatusView(
            ctx.profile.getMachineCode(),
            st.getConnectionStatus(),
            st.getCurrentScenario(),
            st.getMachineState(),
            !st.isSendPaused() && st.getConnectionStatus() != ConnectionStatus.OFFLINE,
            st.getCurrentTemperatureC(),
            st.getCurrentPowerKw(),
            st.getCurrentVibrationMmS(),
            st.getOutputCount(),
            st.getRemainingToolLifePct(),
            formatAge(st.getLastSentTs(), now),
            formatAge(st.getLastSuccessTs(), now),
            st.getLastErrorMessage(),
            ctx.pendingQueue.size(),
            ctx.delayedBuffer.size()
        );
    }

    private String formatAge(Instant timestamp, Instant now) {
        return timestamp == null ? "never" : Duration.between(timestamp, now).toSeconds() + "s ago";
    }

    private String normalizeCode(String machineCode) {
        if (machineCode == null) {
            return null;
        }
        String normalized = machineCode.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    // ---- inner types ----

    private record DelayedEntry(NormalizedTelemetryDto dto, Instant releaseAt) {
    }

    private static class MachineContext {
        MachineProfile profile;
        MachineRuntimeState state;
        final Deque<NormalizedTelemetryDto> pendingQueue = new ArrayDeque<>();
        final List<DelayedEntry> delayedBuffer = new ArrayList<>();

        MachineContext(MachineProfile profile, MachineRuntimeState state) {
            this.profile = profile;
            this.state = state;
        }

        void addToDelayedBuffer(NormalizedTelemetryDto dto, Instant releaseAt) {
            delayedBuffer.add(new DelayedEntry(dto, releaseAt));
        }

        void drainDelayedToQueue(Instant now) {
            Iterator<DelayedEntry> it = delayedBuffer.iterator();
            while (it.hasNext()) {
                DelayedEntry entry = it.next();
                if (!now.isBefore(entry.releaseAt())) {
                    pendingQueue.addLast(entry.dto());
                    it.remove();
                }
            }
        }
    }

    public record MachineStatusView(
        String machineCode,
        ConnectionStatus connectionStatus,
        MachineScenario scenario,
        MachineState machineState,
        boolean sending,
        double temperatureC,
        double powerKw,
        double vibrationMmS,
        long outputCount,
        double remainingToolLifePct,
        String lastSent,
        String lastSuccess,
        String lastError,
        int pendingQueueSize,
        int delayedQueueSize
    ) {
    }
}

