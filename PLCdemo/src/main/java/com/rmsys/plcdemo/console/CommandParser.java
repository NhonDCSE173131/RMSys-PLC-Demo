package com.rmsys.plcdemo.console;

import java.util.Arrays;

public class CommandParser {

    public ParsedCommand parse(String line) {
        if (line == null || line.isBlank()) {
            return new ParsedCommand(CommandType.UNKNOWN, new String[0]);
        }
        String[] tokens = Arrays.stream(line.trim().split("\\s+"))
            .filter(token -> !token.isBlank())
            .toArray(String[]::new);
        if (tokens.length == 0) {
            return new ParsedCommand(CommandType.UNKNOWN, new String[0]);
        }

        return switch (tokens[0].toLowerCase()) {
            case "help", "case", "cases", "option", "options", "scenario", "scenarios", "?" -> new ParsedCommand(CommandType.HELP, tokens);
            case "list", "status" -> new ParsedCommand(CommandType.LIST, tokens);
            case "pause" -> new ParsedCommand(CommandType.PAUSE, tokens);
            case "resume" -> new ParsedCommand(CommandType.RESUME, tokens);
            case "exit" -> new ParsedCommand(CommandType.EXIT, tokens);
            case "machine" -> new ParsedCommand(CommandType.MACHINE, tokens);
            case "all" -> new ParsedCommand(CommandType.ALL, tokens);
            case "script" -> new ParsedCommand(CommandType.SCRIPT, tokens);
            default -> new ParsedCommand(CommandType.UNKNOWN, tokens);
        };
    }

    public enum CommandType {
        HELP,
        LIST,
        MACHINE,
        ALL,
        SCRIPT,
        PAUSE,
        RESUME,
        EXIT,
        UNKNOWN
    }

    public record ParsedCommand(CommandType type, String[] tokens) {
    }
}
