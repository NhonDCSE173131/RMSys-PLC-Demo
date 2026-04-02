package com.rmsys.plcdemo.domain.model;

import java.time.Instant;

public class MachineRuntimeState {
    private ConnectionStatus connectionStatus;
    private MachineState machineState;
    private double currentTemperatureC;
    private double currentVibrationMmS;
    private double currentPowerKw;
    private double currentSpindleSpeedRpm;
    private double currentFeedRateMmMin;
    private long cycleCount;
    private long partCount;
    private long goodCount;
    private long rejectCount;
    private double toolWearPercent;
    private double maintenanceHealthScore;
    private double energyKwhTotal;
    private double energyKwhShift;
    private double energyKwhDay;
    private double runtimeHours;
    private double servoOnHours;
    private double cycleTimeSec;
    private double idealCycleTimeSec;
    private double spindleLoadPct;
    private double servoLoadPct;
    private double cuttingSpeedMMin;
    private double depthOfCutMm;
    private double feedPerToothMm;
    private double widthOfCutMm;
    private double materialRemovalRateCm3Min;
    private double weldingCurrentA;
    private String operationMode;
    private String programName;
    private String toolCode;
    private double voltageV;
    private double currentA;
    private double powerFactor;
    private double frequencyHz;
    private double motorTemperatureC;
    private double bearingTemperatureC;
    private double cabinetTemperatureC;
    private int startStopCount;
    private double lubricationLevelPct;
    private boolean batteryLow;
    private double cycleProgressSec;
    private MachineScenario currentScenario;
    private Instant scenarioUntilTs;
    private Instant lastTelemetryTs;
    private Instant lastSentTs;
    private Instant lastSuccessTs;
    private String lastErrorMessage;
    private boolean sendPaused;
    private boolean duplicatePending;
    private int slowSendIntervalSec;
    private int tickCounter;

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public MachineState getMachineState() {
        return machineState;
    }

    public void setMachineState(MachineState machineState) {
        this.machineState = machineState;
    }

    public double getCurrentTemperatureC() {
        return currentTemperatureC;
    }

    public void setCurrentTemperatureC(double currentTemperatureC) {
        this.currentTemperatureC = currentTemperatureC;
    }

    public double getCurrentVibrationMmS() {
        return currentVibrationMmS;
    }

    public void setCurrentVibrationMmS(double currentVibrationMmS) {
        this.currentVibrationMmS = currentVibrationMmS;
    }

    public double getCurrentPowerKw() {
        return currentPowerKw;
    }

    public void setCurrentPowerKw(double currentPowerKw) {
        this.currentPowerKw = currentPowerKw;
    }

    public double getCurrentSpindleSpeedRpm() {
        return currentSpindleSpeedRpm;
    }

    public void setCurrentSpindleSpeedRpm(double currentSpindleSpeedRpm) {
        this.currentSpindleSpeedRpm = currentSpindleSpeedRpm;
    }

    public double getCurrentFeedRateMmMin() {
        return currentFeedRateMmMin;
    }

    public void setCurrentFeedRateMmMin(double currentFeedRateMmMin) {
        this.currentFeedRateMmMin = currentFeedRateMmMin;
    }

    public long getCycleCount() {
        return cycleCount;
    }

    public void setCycleCount(long cycleCount) {
        this.cycleCount = cycleCount;
    }

    public long getPartCount() {
        return partCount;
    }

    public void setPartCount(long partCount) {
        this.partCount = partCount;
    }

    public long getOutputCount() {
        return partCount;
    }

    public void setOutputCount(long outputCount) {
        this.partCount = outputCount;
    }

    public long getGoodCount() {
        return goodCount;
    }

    public void setGoodCount(long goodCount) {
        this.goodCount = goodCount;
    }

    public long getRejectCount() {
        return rejectCount;
    }

    public void setRejectCount(long rejectCount) {
        this.rejectCount = rejectCount;
    }

    public double getToolWearPercent() {
        return toolWearPercent;
    }

    public void setToolWearPercent(double toolWearPercent) {
        this.toolWearPercent = toolWearPercent;
    }

    public double getMaintenanceHealthScore() {
        return maintenanceHealthScore;
    }

    public void setMaintenanceHealthScore(double maintenanceHealthScore) {
        this.maintenanceHealthScore = maintenanceHealthScore;
    }

    public double getEnergyKwhTotal() {
        return energyKwhTotal;
    }

    public void setEnergyKwhTotal(double energyKwhTotal) {
        this.energyKwhTotal = energyKwhTotal;
    }

    public double getEnergyKwhShift() {
        return energyKwhShift;
    }

    public void setEnergyKwhShift(double energyKwhShift) {
        this.energyKwhShift = energyKwhShift;
    }

    public double getEnergyKwhDay() {
        return energyKwhDay;
    }

    public void setEnergyKwhDay(double energyKwhDay) {
        this.energyKwhDay = energyKwhDay;
    }

    public double getRuntimeHours() {
        return runtimeHours;
    }

    public void setRuntimeHours(double runtimeHours) {
        this.runtimeHours = runtimeHours;
    }

    public double getServoOnHours() {
        return servoOnHours;
    }

    public void setServoOnHours(double servoOnHours) {
        this.servoOnHours = servoOnHours;
    }

    public double getCycleTimeSec() {
        return cycleTimeSec;
    }

    public void setCycleTimeSec(double cycleTimeSec) {
        this.cycleTimeSec = cycleTimeSec;
    }

    public double getIdealCycleTimeSec() {
        return idealCycleTimeSec;
    }

    public void setIdealCycleTimeSec(double idealCycleTimeSec) {
        this.idealCycleTimeSec = idealCycleTimeSec;
    }

    public double getSpindleLoadPct() {
        return spindleLoadPct;
    }

    public void setSpindleLoadPct(double spindleLoadPct) {
        this.spindleLoadPct = spindleLoadPct;
    }

    public double getServoLoadPct() {
        return servoLoadPct;
    }

    public void setServoLoadPct(double servoLoadPct) {
        this.servoLoadPct = servoLoadPct;
    }

    public double getCuttingSpeedMMin() {
        return cuttingSpeedMMin;
    }

    public void setCuttingSpeedMMin(double cuttingSpeedMMin) {
        this.cuttingSpeedMMin = cuttingSpeedMMin;
    }

    public double getDepthOfCutMm() {
        return depthOfCutMm;
    }

    public void setDepthOfCutMm(double depthOfCutMm) {
        this.depthOfCutMm = depthOfCutMm;
    }

    public double getFeedPerToothMm() {
        return feedPerToothMm;
    }

    public void setFeedPerToothMm(double feedPerToothMm) {
        this.feedPerToothMm = feedPerToothMm;
    }

    public double getWidthOfCutMm() {
        return widthOfCutMm;
    }

    public void setWidthOfCutMm(double widthOfCutMm) {
        this.widthOfCutMm = widthOfCutMm;
    }

    public double getMaterialRemovalRateCm3Min() {
        return materialRemovalRateCm3Min;
    }

    public void setMaterialRemovalRateCm3Min(double materialRemovalRateCm3Min) {
        this.materialRemovalRateCm3Min = materialRemovalRateCm3Min;
    }

    public double getWeldingCurrentA() {
        return weldingCurrentA;
    }

    public void setWeldingCurrentA(double weldingCurrentA) {
        this.weldingCurrentA = weldingCurrentA;
    }

    public String getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(String operationMode) {
        this.operationMode = operationMode;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getToolCode() {
        return toolCode;
    }

    public void setToolCode(String toolCode) {
        this.toolCode = toolCode;
    }

    public double getVoltageV() {
        return voltageV;
    }

    public void setVoltageV(double voltageV) {
        this.voltageV = voltageV;
    }

    public double getCurrentA() {
        return currentA;
    }

    public void setCurrentA(double currentA) {
        this.currentA = currentA;
    }

    public double getPowerFactor() {
        return powerFactor;
    }

    public void setPowerFactor(double powerFactor) {
        this.powerFactor = powerFactor;
    }

    public double getFrequencyHz() {
        return frequencyHz;
    }

    public void setFrequencyHz(double frequencyHz) {
        this.frequencyHz = frequencyHz;
    }

    public double getMotorTemperatureC() {
        return motorTemperatureC;
    }

    public void setMotorTemperatureC(double motorTemperatureC) {
        this.motorTemperatureC = motorTemperatureC;
    }

    public double getBearingTemperatureC() {
        return bearingTemperatureC;
    }

    public void setBearingTemperatureC(double bearingTemperatureC) {
        this.bearingTemperatureC = bearingTemperatureC;
    }

    public double getCabinetTemperatureC() {
        return cabinetTemperatureC;
    }

    public void setCabinetTemperatureC(double cabinetTemperatureC) {
        this.cabinetTemperatureC = cabinetTemperatureC;
    }

    public int getStartStopCount() {
        return startStopCount;
    }

    public void setStartStopCount(int startStopCount) {
        this.startStopCount = startStopCount;
    }

    public double getLubricationLevelPct() {
        return lubricationLevelPct;
    }

    public void setLubricationLevelPct(double lubricationLevelPct) {
        this.lubricationLevelPct = lubricationLevelPct;
    }

    public boolean isBatteryLow() {
        return batteryLow;
    }

    public void setBatteryLow(boolean batteryLow) {
        this.batteryLow = batteryLow;
    }

    public double getCycleProgressSec() {
        return cycleProgressSec;
    }

    public void setCycleProgressSec(double cycleProgressSec) {
        this.cycleProgressSec = cycleProgressSec;
    }

    public MachineScenario getCurrentScenario() {
        return currentScenario;
    }

    public void setCurrentScenario(MachineScenario currentScenario) {
        this.currentScenario = currentScenario;
    }

    public Instant getScenarioUntilTs() {
        return scenarioUntilTs;
    }

    public void setScenarioUntilTs(Instant scenarioUntilTs) {
        this.scenarioUntilTs = scenarioUntilTs;
    }

    public Instant getLastTelemetryTs() {
        return lastTelemetryTs;
    }

    public void setLastTelemetryTs(Instant lastTelemetryTs) {
        this.lastTelemetryTs = lastTelemetryTs;
    }

    public Instant getLastSentTs() {
        return lastSentTs;
    }

    public void setLastSentTs(Instant lastSentTs) {
        this.lastSentTs = lastSentTs;
    }

    public Instant getLastSuccessTs() {
        return lastSuccessTs;
    }

    public void setLastSuccessTs(Instant lastSuccessTs) {
        this.lastSuccessTs = lastSuccessTs;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public boolean isSendPaused() {
        return sendPaused;
    }

    public void setSendPaused(boolean sendPaused) {
        this.sendPaused = sendPaused;
    }

    public boolean isDuplicatePending() {
        return duplicatePending;
    }

    public void setDuplicatePending(boolean duplicatePending) {
        this.duplicatePending = duplicatePending;
    }

    public int getSlowSendIntervalSec() {
        return slowSendIntervalSec;
    }

    public void setSlowSendIntervalSec(int slowSendIntervalSec) {
        this.slowSendIntervalSec = slowSendIntervalSec;
    }

    public int getTickCounter() {
        return tickCounter;
    }

    public void setTickCounter(int tickCounter) {
        this.tickCounter = tickCounter;
    }

    public double getRemainingToolLifePct() {
        return Math.max(0, 100 - toolWearPercent);
    }
}

