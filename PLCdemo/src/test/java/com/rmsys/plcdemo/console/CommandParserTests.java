package com.rmsys.plcdemo.console;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandParserTests {

    private final CommandParser parser = new CommandParser();

    @Test
    void parseMachineScenarioCommand() {
        CommandParser.ParsedCommand command = parser.parse("machine MC-01 scenario overheat 30");

        assertEquals(CommandParser.CommandType.MACHINE, command.type());
        assertEquals("MC-01", command.tokens()[1]);
        assertEquals("overheat", command.tokens()[3]);
    }

    @Test
    void parseUnknownCommand() {
        CommandParser.ParsedCommand command = parser.parse("foobar");
        assertEquals(CommandParser.CommandType.UNKNOWN, command.type());
    }

    @Test
    void parseCasesAliasAsHelp() {
        CommandParser.ParsedCommand command = parser.parse("cases");

        assertEquals(CommandParser.CommandType.HELP, command.type());
    }
}

