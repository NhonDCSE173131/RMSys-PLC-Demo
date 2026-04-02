package com.rmsys.plcdemo.console;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Simple Swing GUI console that subscribes to ConsoleIo and allows typing commands.
 */
public class GuiConsole {

    private final JFrame frame;
    private final JTextArea output;
    private final JTextField input;
    private final CommandParser parser = new CommandParser();
    private final CommandHandlers handlers;
    private final ScriptRunner scriptRunner;

    public GuiConsole(CommandHandlers handlers, ScriptRunner scriptRunner) {
        this.handlers = handlers;
        this.scriptRunner = scriptRunner;

        frame = new JFrame("PLC Simulator Console");
        output = new JTextArea(20, 80);
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        input = new JTextField();

        JScrollPane scroll = new JScrollPane(output, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(new JLabel("sim> "), BorderLayout.WEST);
        bottom.add(input, BorderLayout.CENTER);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(bottom, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        input.addActionListener(this::onSubmit);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                ConsoleIo.removeListener(GuiConsole.this::onConsoleLine);
            }
        });

        ConsoleIo.addListener(this::onConsoleLine);
    }

    private void onConsoleLine(String line) {
        SwingUtilities.invokeLater(() -> {
            output.append(line + "\n");
            output.setCaretPosition(output.getDocument().getLength());
        });
    }

    private void onSubmit(ActionEvent ev) {
        String text = input.getText();
        if (text == null) return;
        input.setText("");
        appendLocal("sim> " + text);
        CommandParser.ParsedCommand cmd = parser.parse(text);
        String result = switch (cmd.type()) {
            case HELP -> handlers.help();
            case LIST -> handlers.list();
            case MACHINE -> handlers.machine(cmd.tokens());
            case ALL -> handlers.all(cmd.tokens());
            case SCRIPT -> {
                if (cmd.tokens().length < 2) yield "Usage: script <shift-start|network-chaos|maintenance-warning>";
                yield scriptRunner.run(cmd.tokens()[1]);
            }
            case PAUSE -> { handlers.pause(); yield "Send paused for all machines."; }
            case RESUME -> { handlers.resume(); yield "Send resumed for all machines."; }
            case EXIT -> { System.exit(0); yield ""; }
            case UNKNOWN -> cmd.tokens().length > 0 ? "Unknown command '" + cmd.tokens()[0] + "'. Type 'help'." : "";
        };
        if (result != null && !result.isBlank()) appendLocal(result);
    }

    private void appendLocal(String s) {
        SwingUtilities.invokeLater(() -> {
            output.append(s + "\n");
            output.setCaretPosition(output.getDocument().getLength());
        });
    }

    public void show() {
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }
}

