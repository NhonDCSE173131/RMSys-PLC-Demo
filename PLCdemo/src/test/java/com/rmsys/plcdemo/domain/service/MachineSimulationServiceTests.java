package com.rmsys.plcdemo.domain.service;

import com.rmsys.plcdemo.config.SimulatorProperties;
import com.rmsys.plcdemo.domain.model.MachineProfile;
import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.scenario.ScenarioEngine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rmsys.plcdemo.domain.model.ConnectionStatus;
import com.rmsys.plcdemo.domain.model.MachineState;

class MachineSimulationServiceTests {

    @Test
    void disconnectShouldStopGeneratingAndSendingPackets() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);
        properties.setSimulatorNode("SIM-TEST-01");
        properties.setMachines(List.of(buildProfile()));

        ScenarioEngine scenarioEngine = new ScenarioEngine();
        TelemetryGenerationService telemetryGenerationService = new TelemetryGenerationService(scenarioEngine, properties);
        TelemetrySendService telemetrySendService = mock(TelemetrySendService.class);

        MachineSimulationService service = new MachineSimulationService(
            properties,
            scenarioEngine,
            telemetryGenerationService,
            telemetrySendService
        );
        service.init();

        assertTrue(service.applyScenario("CNC-01", MachineScenario.DISCONNECT, null));
        service.tickAllMachines();

        MachineSimulationService.MachineStatusView status = service.getMachineStatus("CNC-01").orElseThrow();
        assertEquals(0, status.pendingQueueSize());
        assertEquals(0, status.delayedQueueSize());
        verify(telemetrySendService, never()).handleSend(any(), any());
    }

    @Test
    void maintenanceShouldKeepOnlineButNotProduce() {
        SimulatorProperties properties = new SimulatorProperties();
        properties.setTickMs(1000);
        properties.setSimulatorNode("SIM-TEST-01");
        properties.setMachines(List.of(buildProfile()));

        ScenarioEngine scenarioEngine = new ScenarioEngine();
        TelemetryGenerationService telemetryGenerationService = new TelemetryGenerationService(scenarioEngine, properties);
        TelemetrySendService telemetrySendService = mock(TelemetrySendService.class);

        MachineSimulationService service = new MachineSimulationService(
            properties, scenarioEngine, telemetryGenerationService, telemetrySendService
        );
        service.init();

        // Let warmup pass first
        assertTrue(service.applyScenario("CNC-01", MachineScenario.NORMAL, null));
        service.tickAllMachines();

        MachineSimulationService.MachineStatusView beforeMaint = service.getMachineStatus("CNC-01").orElseThrow();
        long outputBefore = beforeMaint.outputCount();

        assertTrue(service.applyScenario("CNC-01", MachineScenario.MAINTENANCE, null));
        service.tickAllMachines();

        MachineSimulationService.MachineStatusView afterMaint = service.getMachineStatus("CNC-01").orElseThrow();
        assertEquals(MachineState.MAINTENANCE, afterMaint.machineState());
        assertEquals(ConnectionStatus.ONLINE, afterMaint.connectionStatus());
        assertEquals(outputBefore, afterMaint.outputCount());
    }

    private MachineProfile buildProfile() {
        MachineProfile p = new MachineProfile();
        p.setEnabled(true);
        p.setMachineId("a1000000-0000-0000-0000-000000000001");
        p.setMachineCode("CNC-01");
        p.setDisplayName("CNC Lathe 01");
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
