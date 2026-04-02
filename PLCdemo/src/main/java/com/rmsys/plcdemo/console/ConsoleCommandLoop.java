package com.rmsys.plcdemo.console;

import com.rmsys.plcdemo.config.SimulatorProperties;
import com.rmsys.plcdemo.domain.service.MachineSimulationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;
import static com.rmsys.plcdemo.console.ConsoleIo.println;
import static com.rmsys.plcdemo.console.ConsoleIo.print;

@Component
public class ConsoleCommandLoop implements ApplicationRunner {

    private final SimulatorProperties properties;
    private final CommandParser parser = new CommandParser();
    private final CommandHandlers handlers;
    private final ScriptRunner scriptRunner;

    public ConsoleCommandLoop(
        SimulatorProperties properties,
        MachineSimulationService simulationService,
        ScriptRunner scriptRunner
    ) {
        this.properties = properties;
        this.handlers = new CommandHandlers(simulationService);
        this.scriptRunner = scriptRunner;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConsoleEnabled()) {
            return;
        }
        if (properties.isGuiEnabled()) {
            // Launch GUI console on EDT
            GuiConsole gui = new GuiConsole(handlers, scriptRunner);
            gui.show();
            return;
        }
        Thread thread = Thread.ofVirtual().name("sim-console-loop").start(this::loop);
    }

    private void loop() {
        println("PLC Simulator ready. Type 'help', 'cases', or 'list' for commands.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                // prompt in a synchronized way so dashboard prints don't break input
                ConsoleIo.setPrompt("sim> ");
                ConsoleIo.beginInput();
                print("sim> ");
                String line;
                try {
                    line = scanner.nextLine();
                } catch (Exception ex) {
                    // scanner may throw if input closed; wait a bit and continue
                    ConsoleIo.endInput();
                    sleepQuietly(300);
                    continue;
                } finally {
                    ConsoleIo.endInput();
                }
                CommandParser.ParsedCommand cmd = parser.parse(line);
                String result = switch (cmd.type()) {
                    case HELP    -> handlers.help();
                    case LIST    -> handlers.list();
                    case MACHINE -> handlers.machine(cmd.tokens());
                    case ALL     -> handlers.all(cmd.tokens());
                    case SCRIPT  -> {
                        if (cmd.tokens().length < 2) yield "Usage: script <shift-start|network-chaos|maintenance-warning>";
                        yield scriptRunner.run(cmd.tokens()[1]);
                    }
                    case PAUSE   -> { handlers.pause(); yield "Send paused for all machines."; }
                    case RESUME  -> { handlers.resume(); yield "Send resumed for all machines."; }
                    case EXIT    -> { println("Exiting simulator..."); System.exit(0); yield ""; }
                    case UNKNOWN -> cmd.tokens().length > 0
                        ? "Unknown command '" + cmd.tokens()[0] + "'. Type 'help'."
                        : "";
                };
                if (!result.isBlank()) {
                    println(result);
                }
            }
        }
    }

    private static void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
