package com.rmsys.plcdemo.domain.model;

public enum MachineScenario {
    NORMAL,
    WARMUP,
    IDLE,
    OVERHEAT,
    HIGH_VIBRATION,
    TOOL_WEAR_RISING,
    EMERGENCY_STOP,
    MAINTENANCE,
    DISCONNECT,
    SLOWSEND,
    DELAYED,
    DUPLICATE;

    public static MachineScenario fromString(String raw) {
        if (raw == null) throw new IllegalArgumentException("Scenario cannot be null");
        return MachineScenario.valueOf(raw.trim().toUpperCase()
            .replace("-", "_").replace(" ", "_"));
    }
}
