package com.rmsys.plcdemo.console;

/**
 * Simple synchronized console IO helper to avoid interleaved writes
 * from multiple threads which can interfere with user typing.
 */
public final class ConsoleIo {
    private static final Object LOCK = new Object();
    private static volatile boolean inputActive = false;
    private static volatile String prompt = "";

    private ConsoleIo() {}

    public static void println(String line) {
        synchronized (LOCK) {
            if (inputActive) {
                // move to new line, print message, then reprint prompt
                System.out.println();
                System.out.println(line);
                notifyListeners(line);
                if (!prompt.isEmpty()) System.out.print(prompt);
            } else {
                System.out.println(line);
                notifyListeners(line);
            }
        }
    }

    public static void print(String s) {
        synchronized (LOCK) {
            System.out.print(s);
        }
    }

    public static void printf(String fmt, Object... args) {
        synchronized (LOCK) {
            if (inputActive) {
                System.out.println();
                System.out.printf(fmt, args);
                notifyListeners(String.format(fmt, args));
                if (!prompt.isEmpty()) System.out.print(prompt);
            } else {
                System.out.printf(fmt, args);
                notifyListeners(String.format(fmt, args));
            }
        }
    }

    public static void setPrompt(String p) {
        prompt = p == null ? "" : p;
    }

    public static void beginInput() {
        synchronized (LOCK) {
            inputActive = true;
        }
    }

    public static void endInput() {
        synchronized (LOCK) {
            inputActive = false;
        }
    }

    // ---- listener support for GUIs ----
    private static final java.util.List<java.util.function.Consumer<String>> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addListener(java.util.function.Consumer<String> listener) {
        if (listener != null) listeners.add(listener);
    }

    public static void removeListener(java.util.function.Consumer<String> listener) {
        if (listener != null) listeners.remove(listener);
    }

    private static void notifyListeners(String message) {
        if (message == null) return;
        for (java.util.function.Consumer<String> l : listeners) {
            try { l.accept(message); } catch (Exception ignored) {}
        }
    }
}




