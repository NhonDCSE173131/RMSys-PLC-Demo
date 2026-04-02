package com.rmsys.plcdemo.console;

import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.service.MachineSimulationService;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public class CommandHandlers {

    private final MachineSimulationService simulationService;

    public CommandHandlers(MachineSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    public String help() {
        String machineList = String.join(" | ", simulationService.availableMachineCodes());
        String scenarioList = supportedScenarios();
        return """
            ── PLC Simulator Commands ──────────────────────────────────────────
            help | cases | options | scenarios        Show usage + supported test cases
            list | status                              All machines status
            pause                                      Pause HTTP send for all
            resume                                     Resume HTTP send for all
            exit                                       Shutdown simulator
            
            machine <code> status                      Single machine status
            machine <code> scenario <name> [durSec]    Set scenario + optional duration
            machine <code> set spindle <rpm>           Override spindle speed
            machine <code> set feed <mm/min>           Override feed rate
            machine <code> set toolwear <0-100>        Override tool wear percent
            machine <code> reconnect                   Force machine back online
            machine <code> reset                       Reset all state to defaults
            
            all scenario <name> [durSec]               Apply scenario to ALL machines
            all pause-send                             Pause send for all
            all resume-send                            Resume send for all
            
            script shift-start                         Run shift-start sequence
            script network-chaos                       Run network chaos sequence
            script maintenance-warning                 Run maintenance warning sequence

            Machines: """ + machineList + """
            
            Scenarios: """ + scenarioList + """

            States: RUNNING | WARMUP | IDLE | STOPPED | EMERGENCY_STOP | MAINTENANCE

            Invalid input tips:
            - machine code is case-insensitive (ex: mc-01)
            - duration must be an integer >= 0
            - toolwear must stay in range 0..100
            
            Realtime 1s test tips:
            - prefer: all scenario normal
            - avoid during smoothness checks: disconnect | slowsend | delayed | duplicate
            - use maintenance to test non-producing online state
            ────────────────────────────────────────────────────────────────────""";
    }

    public String list() {
        StringBuilder sb = new StringBuilder();
        simulationService.listStatuses().forEach(row -> sb.append(formatRow(row)).append('\n'));
        return sb.isEmpty() ? "(no machines)" : sb.toString().stripTrailing();
    }

    public String machineStatus(String code) {
        return simulationService.getMachineStatus(code)
            .map(this::formatRow)
            .orElse("Machine not found: " + code);
    }

    public String machine(String[] tokens) {
        // machine <code> <op> [args...]
        if (tokens.length < 3) {
            return "Usage: machine <code> status|scenario|set|reconnect|reset";
        }
        String code = normalizeMachineCode(tokens[1]);
        String op = tokens[2].toLowerCase(Locale.ROOT);

        return switch (op) {
            case "status" -> machineStatus(code);
            case "scenario" -> {
                if (tokens.length < 4) yield "Missing scenario name.";
                try {
                    MachineScenario scenario = parseScenario(tokens[3]);
                    Integer duration = parseDuration(tokens.length >= 5 ? tokens[4] : null);
                    boolean ok = simulationService.applyScenario(code, scenario, duration);
                    yield ok ? "OK → " + code + " scenario=" + scenario + (duration != null ? " for " + duration + "s" : "")
                        : "Machine not found: " + code;
                } catch (IllegalArgumentException ex) {
                    yield ex.getMessage();
                }
            }
            case "set" -> {
                if (tokens.length < 5) yield "Usage: machine <code> set spindle|feed|toolwear <value>";
                String param = tokens[3].toLowerCase(Locale.ROOT);
                if (!param.matches("spindle|feed|toolwear")) {
                    yield "Unknown param '" + param + "'. Use spindle|feed|toolwear";
                }
                double value;
                try { value = Double.parseDouble(tokens[4]); }
                catch (NumberFormatException e) { yield "Invalid number: " + tokens[4]; }
                if (param.equals("toolwear") && (value < 0 || value > 100)) {
                    yield "toolwear must be between 0 and 100.";
                }
                if ((param.equals("spindle") || param.equals("feed")) && value < 0) {
                    yield param + " must be >= 0.";
                }
                boolean ok = switch (param) {
                    case "spindle" -> simulationService.setSpindleSpeed(code, value);
                    case "feed" -> simulationService.setFeedRate(code, value);
                    case "toolwear" -> simulationService.setToolWear(code, value);
                    default -> throw new IllegalStateException("Unexpected value: " + param);
                };
                if (!ok) yield "Machine not found: " + code;
                yield "OK → " + code + " " + param + "=" + value;
            }
            case "reconnect" -> simulationService.reconnect(code) ? "OK → " + code + " is back ONLINE"
                : "Machine not found: " + code;
            case "reset" -> simulationService.resetMachine(code) ? "OK → " + code + " reset to defaults"
                : "Machine not found: " + code;
            default -> "Unknown machine operation: " + op + ". Type 'help'.";
        };
    }

    public String all(String[] tokens) {
        // all scenario <name> [dur] | all pause-send | all resume-send
        if (tokens.length < 2) return "Usage: all scenario <name> [durSec] | all pause-send | all resume-send";
        String op = tokens[1].toLowerCase(Locale.ROOT);
        return switch (op) {
            case "scenario" -> {
                if (tokens.length < 3) yield "Missing scenario name.";
                try {
                    MachineScenario scenario = parseScenario(tokens[2]);
                    Integer duration = parseDuration(tokens.length >= 4 ? tokens[3] : null);
                    simulationService.applyScenarioAll(scenario, duration);
                    yield "OK → All machines scenario=" + scenario + (duration != null ? " for " + duration + "s" : "");
                } catch (IllegalArgumentException ex) {
                    yield ex.getMessage();
                }
            }
            case "pause-send" -> { simulationService.pauseAllSend(); yield "Send paused for all machines."; }
            case "resume-send" -> { simulationService.resumeAllSend(); yield "Send resumed for all machines."; }
            default -> "Unknown 'all' operation: " + op;
        };
    }

    public void pause() { simulationService.pauseAllSend(); }
    public void resume() { simulationService.resumeAllSend(); }

    // ---- private ----

    private String formatRow(MachineSimulationService.MachineStatusView row) {
        return String.format(
            "[%s] %s | Sending=%s | %s | %s | Temp=%.1fC | Power=%.1fkW | Vib=%.2f | Output=%d | ToolLife=%.1f%% | LastSent=%s | LastOk=%s | Pending=%d | Delayed=%d%s",
            row.machineCode(), row.connectionStatus(), row.sending(), row.scenario(), row.machineState(),
            row.temperatureC(), row.powerKw(), row.vibrationMmS(),
            row.outputCount(), row.remainingToolLifePct(), row.lastSent(), row.lastSuccess(),
            row.pendingQueueSize(), row.delayedQueueSize(),
            row.lastError() == null ? "" : " | LastErr=" + row.lastError());
    }

    private Integer parseDuration(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < 0) {
                throw new IllegalArgumentException("Duration must be an integer >= 0.");
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Duration must be an integer >= 0.");
        }
    }

    private MachineScenario parseScenario(String raw) {
        try {
            return MachineScenario.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown scenario '" + raw + "'. Supported: " + supportedScenarios());
        }
    }

    private String normalizeMachineCode(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    private String supportedScenarios() {
        return Arrays.stream(MachineScenario.values())
            .map(value -> value.name().toLowerCase(Locale.ROOT).replace('_', '-'))
            .collect(Collectors.joining(" | "));
    }
}
