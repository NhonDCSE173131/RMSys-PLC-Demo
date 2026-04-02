package com.rmsys.plcdemo.domain.model;

import java.time.Instant;
import java.util.Map;

public record NormalizedTelemetryDto(
    String machineId,
    String machineCode,
    Instant ts,
    ConnectionStatus connectionStatus,
    MachineState machineState,
    String operationMode,
    String programName,
    boolean cycleRunning,
    double powerKw,
    double temperatureC,
    double vibrationMmS,
    double runtimeHours,
    double cycleTimeSec,
    Double idealCycleTimeSec,
    long outputCount,
    long goodCount,
    long rejectCount,
    double spindleSpeedRpm,
    Double spindleLoadPct,
    Double servoLoadPct,
    double feedRateMmMin,
    Double cuttingSpeedMMin,
    Double depthOfCutMm,
    Double feedPerToothMm,
    Double widthOfCutMm,
    Double materialRemovalRateCm3Min,
    Double weldingCurrentA,
    String toolCode,
    double remainingToolLifePct,
    double voltageV,
    double currentA,
    double powerFactor,
    double frequencyHz,
    double energyKwhShift,
    double energyKwhDay,
    double motorTemperatureC,
    double bearingTemperatureC,
    double cabinetTemperatureC,
    double servoOnHours,
    int startStopCount,
    double lubricationLevelPct,
    boolean batteryLow,
    Map<String, Object> metadata
) {

    public String debugMachineCode() {
        Object code = metadata == null ? null : metadata.get("machineCode");
        return code == null ? machineId : String.valueOf(code);
    }
}

