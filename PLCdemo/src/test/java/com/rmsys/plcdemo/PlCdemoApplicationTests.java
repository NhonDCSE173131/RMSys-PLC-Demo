package com.rmsys.plcdemo;

import com.rmsys.plcdemo.domain.service.MachineSimulationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
    "simulator.console-enabled=false"
})
class PlCdemoApplicationTests {

    @Autowired
    private MachineSimulationService machineSimulationService;

    @Test
    void contextLoads() {
    }

    @Test
    void defaultRegistryShouldExposeFiveBackendAlignedMachines() {
        assertEquals(5, machineSimulationService.availableMachineCodes().size());
    }

}
