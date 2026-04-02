package com.rmsys.plcdemo.console;

import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.service.MachineSimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.rmsys.plcdemo.console.ConsoleIo.println;
import static com.rmsys.plcdemo.console.ConsoleIo.printf;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(ScriptRunner.class);

    private final MachineSimulationService simulationService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ScriptRunner(MachineSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * Runs a named predefined script in a background daemon thread.
     * Returns a status message immediately; script executes asynchronously.
     */
    public String run(String name) {
        if (running.get()) {
            return "A script is already running. Wait for it to finish.";
        }
        List<ScriptStep> steps = switch (name.toLowerCase().replace("-", "_").replace(" ", "_")) {
            case "shift_start" -> shiftStart();
            case "network_chaos" -> networkChaos();
            case "maintenance_warning" -> maintenanceWarning();
            default -> null;
        };
        if (steps == null) {
            return "Unknown script '" + name + "'. Available: shift-start | network-chaos | maintenance-warning";
        }
        Thread thread = Thread.ofVirtual().name("script-" + name).start(() -> executeSteps(steps));
        return "Script '" + name + "' started (" + steps.size() + " steps) in background.";
    }

    // ─── Script definitions ───────────────────────────────────────────────────

    private List<ScriptStep> shiftStart() {
        // Simulates a production shift: warmup → normal → tool wear → overheat → recover
        return List.of(
            new ScriptStep("all scenario warmup 30",       null, MachineScenario.WARMUP,           null, 30),
            new ScriptStep("all scenario normal 60",       null, MachineScenario.NORMAL,           null, 60),
            new ScriptStep("all scenario tool-wear-rising 30", null, MachineScenario.TOOL_WEAR_RISING, null, 30),
            new ScriptStep("all scenario overheat 20",     null, MachineScenario.OVERHEAT,         null, 20),
            new ScriptStep("all scenario normal 60",       null, MachineScenario.NORMAL,           null, 0)
        );
    }

    private List<ScriptStep> networkChaos() {
        // Each machine gets a different network disruption scenario
        List<MachineSimulationService.MachineStatusView> machines = simulationService.listStatuses();
        if (machines.isEmpty()) return List.of();
        // Assign different chaos scenarios to first 4 machines (or fewer)
        MachineScenario[] chaosScenarios = {
            MachineScenario.SLOWSEND,
            MachineScenario.DISCONNECT,
            MachineScenario.DELAYED,
            MachineScenario.DUPLICATE
        };
        // First step: assign chaos scenarios
        // Then recover all after total time
        var stepsBuilder = new java.util.ArrayList<ScriptStep>();
        int count = Math.min(machines.size(), chaosScenarios.length);
        for (int i = 0; i < count; i++) {
            stepsBuilder.add(new ScriptStep(
                machines.get(i).machineCode() + " → " + chaosScenarios[i].name(),
                machines.get(i).machineCode(), chaosScenarios[i], 60, 0));
        }
        stepsBuilder.add(new ScriptStep("wait 60s", null, null, null, 60));
        stepsBuilder.add(new ScriptStep("all → normal", null, MachineScenario.NORMAL, null, 0));
        return stepsBuilder;
    }

    private List<ScriptStep> maintenanceWarning() {
        // Gradually raise tool wear and vibration to trigger maintenance alarm
        return List.of(
            new ScriptStep("all scenario normal 30",        null, MachineScenario.NORMAL,           null, 30),
            new ScriptStep("all scenario tool-wear-rising 60", null, MachineScenario.TOOL_WEAR_RISING, null, 60),
            new ScriptStep("all scenario high-vibration 30",   null, MachineScenario.HIGH_VIBRATION, null, 30),
            new ScriptStep("all scenario overheat 20",         null, MachineScenario.OVERHEAT,       null, 20),
            new ScriptStep("all scenario normal 0",            null, MachineScenario.NORMAL,         null, 0)
        );
    }

    // ─── Execution engine ─────────────────────────────────────────────────────

    private void executeSteps(List<ScriptStep> steps) {
        running.set(true);
        try {
            for (ScriptStep step : steps) {
                log.info("[SCRIPT] Executing: {}", step.label());
                printf("%n[SCRIPT] %s%n", step.label());
                if (step.machineCode() != null && step.scenario() != null) {
                    simulationService.applyScenario(step.machineCode(), step.scenario(), step.durationSec());
                } else if (step.scenario() != null) {
                    simulationService.applyScenarioAll(step.scenario(), step.durationSec());
                }
                int waitSec = step.waitAfterSec();
                if (waitSec > 0) {
                    sleepSeconds(waitSec);
                }
            }
            println("[SCRIPT] Completed.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            println("[SCRIPT] Interrupted.");
        } finally {
            running.set(false);
        }
    }

    private static void sleepSeconds(int sec) throws InterruptedException {
        Thread.sleep(sec * 1000L);
    }

    // ─── Data ─────────────────────────────────────────────────────────────────

    /**
     * @param label       Human-readable step description
     * @param machineCode null means apply to ALL machines
     * @param scenario    null means wait-only step
     * @param durationSec duration of the scenario (null = permanent)
     * @param waitAfterSec sleep after applying the scenario
     */
    private record ScriptStep(
        String label,
        String machineCode,
        MachineScenario scenario,
        Integer durationSec,
        int waitAfterSec
    ) {
    }
}

