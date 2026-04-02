package com.rmsys.plcdemo.domain.service;

import com.rmsys.plcdemo.config.SimulatorProperties;
import com.rmsys.plcdemo.domain.model.ConnectionStatus;
import com.rmsys.plcdemo.domain.model.MachineProfile;
import com.rmsys.plcdemo.domain.model.MachineRuntimeState;
import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.model.MachineState;
import com.rmsys.plcdemo.domain.model.NormalizedTelemetryDto;
import com.rmsys.plcdemo.domain.scenario.ScenarioEngine;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryGenerationServiceTests {

    @Test
    void tickShouldGenerateLogicalTelemetry() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);
        properties.setSimulatorNode("SIM-TEST-01");

        TelemetryGenerationService service = new TelemetryGenerationService(new ScenarioEngine(), properties);
        MachineProfile p = new MachineProfile();
        p.setMachineId("a1000000-0000-0000-0000-000000000001");
        p.setMachineCode("CNC-01");
        p.setMachineType("CNC_MACHINE");
        p.setProgramName("JOB-AXLE-01");
        p.setToolCode("T-AXLE-01");
        p.setMinIdlePowerKw(1.2);
        p.setNominalRunPowerKw(4.8);
        p.setMaxPowerKw(6.8);
        p.setNormalTempMinC(38);
        p.setNormalTempMaxC(55);
        p.setWarningTemp(68);
        p.setDangerTemp(82);
        p.setNormalVibrationMin(1.1);
        p.setNormalVibrationMax(2.2);
        p.setMaxSpindleSpeed(6000);
        p.setNormalFeedRate(850);
        p.setBaseCycleTimeSec(42);
        p.setToolWearRatePerCycle(0.08);
        p.setMaintenanceDecayRate(0.01);

        MachineRuntimeState state = new MachineRuntimeState();
        state.setConnectionStatus(ConnectionStatus.ONLINE);
        state.setMachineState(MachineState.RUNNING);
        state.setCurrentScenario(MachineScenario.NORMAL);
        state.setOperationMode("AUTO");
        state.setCurrentTemperatureC(38);
        state.setCurrentPowerKw(1.2);
        state.setCurrentVibrationMmS(1.1);
        state.setCurrentSpindleSpeedRpm(0);
        state.setCurrentFeedRateMmMin(0);
        state.setMaintenanceHealthScore(100);
        state.setToolWearPercent(2);
        state.setVoltageV(380.0);
        state.setFrequencyHz(50.0);
        state.setPowerFactor(0.95);
        state.setLubricationLevelPct(92.0);
        state.setBatteryLow(false);

        NormalizedTelemetryDto dto = service.tick(p, state, Instant.now());

        assertEquals("a1000000-0000-0000-0000-000000000001", dto.machineId());
        assertEquals("CNC-01", dto.machineCode());
        assertEquals("AUTO", dto.operationMode());
        assertEquals("JOB-AXLE-01", dto.programName());
        assertEquals("T-AXLE-01", dto.toolCode());
        assertNotNull(dto.metadata());
        assertEquals("CNC-01", dto.metadata().get("machineCode"));
        assertEquals("SIM-TEST-01", dto.metadata().get("simulatorNode"));
        assertEquals("CNC_MACHINE", dto.metadata().get("machineCategory"));
        assertEquals(1000L, dto.metadata().get("sampleIntervalMs"));
        assertEquals("STABLE", dto.metadata().get("qualityHint"));
        assertEquals("STEADY_RUN", dto.metadata().get("scenarioPhase"));
        assertTrue(dto.temperatureC() >= 20 && dto.temperatureC() <= 90);
        assertTrue(dto.powerKw() >= 0 && dto.powerKw() <= 6.8);
        assertTrue(dto.vibrationMmS() >= 0 && dto.vibrationMmS() <= 6);
        assertNotNull(dto.idealCycleTimeSec());
        assertNotNull(dto.spindleLoadPct());
        assertNotNull(dto.servoLoadPct());
        assertNotNull(dto.cuttingSpeedMMin());
        assertNotNull(dto.depthOfCutMm());
        assertNotNull(dto.feedPerToothMm());
        assertNotNull(dto.widthOfCutMm());
        assertNotNull(dto.materialRemovalRateCm3Min());
        assertTrue(dto.idealCycleTimeSec() >= 5);
        assertTrue(dto.spindleLoadPct() >= 0 && dto.spindleLoadPct() <= 100);
        assertTrue(dto.servoLoadPct() >= 0 && dto.servoLoadPct() <= 100);
        assertTrue(dto.remainingToolLifePct() >= 0 && dto.remainingToolLifePct() <= 100);
        assertTrue(dto.energyKwhShift() >= 0);
        assertTrue(dto.energyKwhDay() >= 0);
        assertTrue(dto.weldingCurrentA() == null);
        assertFalse(dto.batteryLow());
    }

    @Test
    void tickShouldEmitNullForNonApplicableProcessFields() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);

        TelemetryGenerationService service = new TelemetryGenerationService(new ScenarioEngine(), properties);
        MachineProfile p = new MachineProfile();
        p.setMachineId("a1000000-0000-0000-0000-000000000007");
        p.setMachineCode("MC-07");
        p.setMachineType("INSPECTION_CELL");
        p.setProgramName("JOB-INSPECT-07");
        p.setToolCode("CAM-INSPECT-07");
        p.setMinIdlePowerKw(0.4);
        p.setNominalRunPowerKw(1.6);
        p.setMaxPowerKw(2.5);
        p.setNormalTempMinC(26);
        p.setNormalTempMaxC(38);
        p.setWarningTemp(48);
        p.setDangerTemp(58);
        p.setNormalVibrationMin(0.2);
        p.setNormalVibrationMax(0.7);
        p.setMaxSpindleSpeed(1200);
        p.setNormalFeedRate(180);
        p.setBaseCycleTimeSec(16);
        p.setToolWearRatePerCycle(0.03);
        p.setMaintenanceDecayRate(0.006);

        MachineRuntimeState state = new MachineRuntimeState();
        state.setConnectionStatus(ConnectionStatus.ONLINE);
        state.setMachineState(MachineState.RUNNING);
        state.setCurrentScenario(MachineScenario.NORMAL);
        state.setOperationMode("AUTO");
        state.setCurrentTemperatureC(26);
        state.setCurrentPowerKw(0.4);
        state.setCurrentVibrationMmS(0.2);
        state.setCurrentSpindleSpeedRpm(0);
        state.setCurrentFeedRateMmMin(0);
        state.setMaintenanceHealthScore(100);
        state.setToolWearPercent(2);

        NormalizedTelemetryDto dto = service.tick(p, state, Instant.now());

        assertNotNull(dto.idealCycleTimeSec());
        assertNull(dto.spindleLoadPct());
        assertNull(dto.servoLoadPct());
        assertNull(dto.cuttingSpeedMMin());
        assertNull(dto.depthOfCutMm());
        assertNull(dto.feedPerToothMm());
        assertNull(dto.widthOfCutMm());
        assertNull(dto.materialRemovalRateCm3Min());
        assertNull(dto.weldingCurrentA());
    }

    @Test
    void tickShouldKeepRobotOnlyWithServoSignalsAndNoMachiningSignals() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);

        TelemetryGenerationService service = new TelemetryGenerationService(new ScenarioEngine(), properties);
        MachineProfile p = new MachineProfile();
        p.setMachineId("a1000000-0000-0000-0000-000000000003");
        p.setMachineCode("ROB-01");
        p.setMachineType("ROBOT_ONLY");
        p.setProgramName("JOB-PICKPLACE-03");
        p.setToolCode("EOAT-GRIP-03");
        p.setMinIdlePowerKw(0.8);
        p.setNominalRunPowerKw(2.4);
        p.setMaxPowerKw(3.6);
        p.setNormalTempMinC(30);
        p.setNormalTempMaxC(42);
        p.setWarningTemp(55);
        p.setDangerTemp(68);
        p.setNormalVibrationMin(0.3);
        p.setNormalVibrationMax(0.9);
        p.setMaxSpindleSpeed(2000);
        p.setNormalFeedRate(400);
        p.setBaseCycleTimeSec(18);
        p.setToolWearRatePerCycle(0.04);
        p.setMaintenanceDecayRate(0.008);

        MachineRuntimeState state = new MachineRuntimeState();
        state.setConnectionStatus(ConnectionStatus.ONLINE);
        state.setMachineState(MachineState.RUNNING);
        state.setCurrentScenario(MachineScenario.NORMAL);
        state.setOperationMode("AUTO");
        state.setCurrentTemperatureC(30);
        state.setCurrentPowerKw(1.2);
        state.setCurrentVibrationMmS(0.3);
        state.setCurrentSpindleSpeedRpm(0);
        state.setCurrentFeedRateMmMin(0);
        state.setMaintenanceHealthScore(100);
        state.setToolWearPercent(2);

        NormalizedTelemetryDto dto = service.tick(p, state, Instant.now());

        assertTrue(dto.cycleRunning());
        assertNotNull(dto.servoLoadPct());
        assertNull(dto.spindleLoadPct());
        assertNull(dto.cuttingSpeedMMin());
        assertNull(dto.depthOfCutMm());
        assertNull(dto.feedPerToothMm());
        assertNull(dto.widthOfCutMm());
        assertNull(dto.materialRemovalRateCm3Min());
        assertEquals("ROBOT_ONLY", dto.metadata().get("machineCategory"));
    }

    @Test
    void maintenanceScenarioShouldFreezeCountersAndNotCycleRun() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);

        TelemetryGenerationService service = new TelemetryGenerationService(new ScenarioEngine(), properties);
        MachineProfile p = buildCncProfile();

        MachineRuntimeState state = new MachineRuntimeState();
        state.setConnectionStatus(ConnectionStatus.ONLINE);
        state.setMachineState(MachineState.MAINTENANCE);
        state.setCurrentScenario(MachineScenario.MAINTENANCE);
        state.setOperationMode("MANUAL");
        state.setCurrentTemperatureC(38);
        state.setCurrentPowerKw(1.2);
        state.setCurrentVibrationMmS(1.1);
        state.setCurrentSpindleSpeedRpm(500);
        state.setCurrentFeedRateMmMin(100);
        state.setMaintenanceHealthScore(100);
        state.setToolWearPercent(2);
        state.setOutputCount(100);
        state.setGoodCount(98);
        state.setRejectCount(2);

        NormalizedTelemetryDto dto = service.tick(p, state, Instant.now());

        assertFalse(dto.cycleRunning());
        assertEquals(100, dto.outputCount());
        assertEquals(98, dto.goodCount());
        assertEquals(2, dto.rejectCount());
        assertEquals("MAINTENANCE", dto.metadata().get("scenarioPhase"));
    }

    @Test
    void idleScenarioShouldFreezeCounters() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);

        TelemetryGenerationService service = new TelemetryGenerationService(new ScenarioEngine(), properties);
        MachineProfile p = buildCncProfile();

        MachineRuntimeState state = new MachineRuntimeState();
        state.setConnectionStatus(ConnectionStatus.ONLINE);
        state.setMachineState(MachineState.IDLE);
        state.setCurrentScenario(MachineScenario.IDLE);
        state.setOperationMode("MANUAL");
        state.setCurrentTemperatureC(38);
        state.setCurrentPowerKw(0.5);
        state.setCurrentVibrationMmS(0.5);
        state.setCurrentSpindleSpeedRpm(500);
        state.setCurrentFeedRateMmMin(100);
        state.setMaintenanceHealthScore(100);
        state.setToolWearPercent(5);
        state.setOutputCount(50);
        state.setGoodCount(48);
        state.setRejectCount(2);

        NormalizedTelemetryDto dto = service.tick(p, state, Instant.now());

        assertFalse(dto.cycleRunning());
        assertEquals(50, dto.outputCount());
        assertEquals(48, dto.goodCount());
        assertEquals(2, dto.rejectCount());
    }

    @Test
    void counterInvariantGoodPlusRejectNeverExceedsOutput() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);

        TelemetryGenerationService service = new TelemetryGenerationService(new ScenarioEngine(), properties);
        MachineProfile p = buildCncProfile();

        MachineRuntimeState state = new MachineRuntimeState();
        state.setConnectionStatus(ConnectionStatus.ONLINE);
        state.setMachineState(MachineState.RUNNING);
        state.setCurrentScenario(MachineScenario.OVERHEAT);
        state.setOperationMode("AUTO");
        state.setCurrentTemperatureC(85);
        state.setCurrentPowerKw(5.0);
        state.setCurrentVibrationMmS(2.0);
        state.setCurrentSpindleSpeedRpm(3000);
        state.setCurrentFeedRateMmMin(500);
        state.setMaintenanceHealthScore(50);
        state.setToolWearPercent(80);
        state.setOutputCount(10);
        state.setGoodCount(8);
        state.setRejectCount(2);

        for (int i = 0; i < 200; i++) {
            NormalizedTelemetryDto dto = service.tick(p, state, Instant.now());
            assertTrue(dto.goodCount() + dto.rejectCount() <= dto.outputCount(),
                "Invariant violated at tick " + i + ": good=" + dto.goodCount() + " reject=" + dto.rejectCount() + " output=" + dto.outputCount());
            assertTrue(dto.goodCount() >= 0, "goodCount must be >= 0");
            assertTrue(dto.rejectCount() >= 0, "rejectCount must be >= 0");
        }
    }

    private MachineProfile buildCncProfile() {
        MachineProfile p = new MachineProfile();
        p.setMachineId("a1000000-0000-0000-0000-000000000001");
        p.setMachineCode("CNC-01");
        p.setMachineType("CNC_MACHINE");
        p.setProgramName("JOB-AXLE-01");
        p.setToolCode("T-AXLE-01");
        p.setMinIdlePowerKw(1.2);
        p.setNominalRunPowerKw(4.8);
        p.setMaxPowerKw(6.8);
        p.setNormalTempMinC(38);
        p.setNormalTempMaxC(55);
        p.setWarningTemp(68);
        p.setDangerTemp(82);
        p.setNormalVibrationMin(1.1);
        p.setNormalVibrationMax(2.2);
        p.setMaxSpindleSpeed(6000);
        p.setNormalFeedRate(850);
        p.setBaseCycleTimeSec(42);
        p.setToolWearRatePerCycle(0.08);
        p.setMaintenanceDecayRate(0.01);
        return p;
    }
}

