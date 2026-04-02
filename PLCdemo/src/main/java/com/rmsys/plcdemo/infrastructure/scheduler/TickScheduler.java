package com.rmsys.plcdemo.infrastructure.scheduler;

import com.rmsys.plcdemo.domain.service.MachineSimulationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TickScheduler {

    private final MachineSimulationService machineSimulationService;

    public TickScheduler(MachineSimulationService machineSimulationService) {
        this.machineSimulationService = machineSimulationService;
    }

    @Scheduled(fixedRateString = "${simulator.tick-ms:1000}")
    public void runTick() {
        machineSimulationService.tickAllMachines();
    }
}

