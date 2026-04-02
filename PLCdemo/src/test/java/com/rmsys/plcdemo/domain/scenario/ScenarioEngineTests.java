package com.rmsys.plcdemo.domain.scenario;

import com.rmsys.plcdemo.domain.model.ConnectionStatus;
import com.rmsys.plcdemo.domain.model.MachineProfile;
import com.rmsys.plcdemo.domain.model.MachineRuntimeState;
import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.model.MachineState;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScenarioEngineTests {

    private final ScenarioEngine engine = new ScenarioEngine();

    @Test
    void normalTargetShouldDisableMachiningSignalsForRobotOnly() {
        MachineProfile profile = new MachineProfile();
        profile.setMachineType("ROBOT_ONLY");
        profile.setNormalTempMinC(30);
        profile.setNormalTempMaxC(42);
        profile.setNormalVibrationMin(0.3);
        profile.setNormalVibrationMax(0.9);
        profile.setNominalRunPowerKw(2.4);
        profile.setMaxSpindleSpeed(2000);
        profile.setNormalFeedRate(400);

        MachineRuntimeState state = new MachineRuntimeState();
        state.setCurrentScenario(MachineScenario.NORMAL);

        ScenarioEngine.ScenarioTargets targets = engine.targets(profile, state);

        assertEquals(0.0, targets.spindleTarget());
        assertEquals(0.0, targets.feedTarget());
        assertTrue(targets.targetPower() > 0.0);
    }

    @Test
    void normalTargetShouldKeepMachiningSignalsForCnc() {
        MachineProfile profile = new MachineProfile();
        profile.setMachineType("CNC_MACHINE");
        profile.setNormalTempMinC(38);
        profile.setNormalTempMaxC(55);
        profile.setNormalVibrationMin(1.1);
        profile.setNormalVibrationMax(2.2);
        profile.setNominalRunPowerKw(4.8);
        profile.setMaxSpindleSpeed(6000);
        profile.setNormalFeedRate(850);

        MachineRuntimeState state = new MachineRuntimeState();
        state.setCurrentScenario(MachineScenario.NORMAL);

        ScenarioEngine.ScenarioTargets targets = engine.targets(profile, state);

        assertTrue(targets.spindleTarget() > 0.0);
        assertTrue(targets.feedTarget() > 0.0);
    }

    @Test
    void maintenanceTargetShouldHaveZeroSpindleAndFeed() {
        MachineProfile profile = new MachineProfile();
        profile.setMachineType("CNC_MACHINE");
        profile.setNormalTempMinC(38);
        profile.setNormalTempMaxC(55);
        profile.setMinIdlePowerKw(1.2);
        profile.setNormalVibrationMin(1.1);
        profile.setNormalVibrationMax(2.2);
        profile.setNominalRunPowerKw(4.8);
        profile.setMaxSpindleSpeed(6000);
        profile.setNormalFeedRate(850);

        MachineRuntimeState state = new MachineRuntimeState();
        state.setCurrentScenario(MachineScenario.MAINTENANCE);

        ScenarioEngine.ScenarioTargets targets = engine.targets(profile, state);

        assertEquals(0.0, targets.spindleTarget());
        assertEquals(0.0, targets.feedTarget());
        assertTrue(targets.targetPower() > 0.0);
        assertEquals(0.0, targets.wearMultiplier());
    }

    @Test
    void refreshScenarioMaintenanceShouldSetCorrectState() {
        MachineRuntimeState state = new MachineRuntimeState();
        state.setCurrentScenario(MachineScenario.MAINTENANCE);

        engine.refreshScenario(state, Instant.now());

        assertEquals(MachineState.MAINTENANCE, state.getMachineState());
        assertEquals(ConnectionStatus.ONLINE, state.getConnectionStatus());
        assertEquals("MANUAL", state.getOperationMode());
    }
}

