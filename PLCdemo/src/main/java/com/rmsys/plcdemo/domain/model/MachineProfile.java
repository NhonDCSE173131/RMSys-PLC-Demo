package com.rmsys.plcdemo.domain.model;

public class MachineProfile {
    private boolean enabled = true;
    private String machineId;
    private String machineCode;
    private String displayName;
    private String machineType;
    private String programName;
    private String toolCode;
    private double minIdlePowerKw;
    private double nominalRunPowerKw;
    private double maxPowerKw;
    private double normalTempMinC;
    private double normalTempMaxC;
    private double warningTemp;
    private double dangerTemp;
    private double normalVibrationMin;
    private double normalVibrationMax;
    private double maxSpindleSpeed;
    private double normalFeedRate;
    private int baseCycleTimeSec;
    private double toolWearRatePerCycle;
    private double maintenanceDecayRate;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineCode() {
        return machineCode;
    }

    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMachineType() {
        return machineType;
    }

    public void setMachineType(String machineType) {
        this.machineType = machineType;
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

    public double getMinIdlePowerKw() {
        return minIdlePowerKw;
    }

    public void setMinIdlePowerKw(double minIdlePowerKw) {
        this.minIdlePowerKw = minIdlePowerKw;
    }

    public double getNominalRunPowerKw() {
        return nominalRunPowerKw;
    }

    public void setNominalRunPowerKw(double nominalRunPowerKw) {
        this.nominalRunPowerKw = nominalRunPowerKw;
    }

    public double getMaxPowerKw() {
        return maxPowerKw;
    }

    public void setMaxPowerKw(double maxPowerKw) {
        this.maxPowerKw = maxPowerKw;
    }

    public double getNormalTempMinC() {
        return normalTempMinC;
    }

    public void setNormalTempMinC(double normalTempMinC) {
        this.normalTempMinC = normalTempMinC;
    }

    public double getNormalTempMaxC() {
        return normalTempMaxC;
    }

    public void setNormalTempMaxC(double normalTempMaxC) {
        this.normalTempMaxC = normalTempMaxC;
    }

    public double getWarningTemp() {
        return warningTemp;
    }

    public void setWarningTemp(double warningTemp) {
        this.warningTemp = warningTemp;
    }

    public double getDangerTemp() {
        return dangerTemp;
    }

    public void setDangerTemp(double dangerTemp) {
        this.dangerTemp = dangerTemp;
    }

    public double getNormalVibrationMin() {
        return normalVibrationMin;
    }

    public void setNormalVibrationMin(double normalVibrationMin) {
        this.normalVibrationMin = normalVibrationMin;
    }

    public double getNormalVibrationMax() {
        return normalVibrationMax;
    }

    public void setNormalVibrationMax(double normalVibrationMax) {
        this.normalVibrationMax = normalVibrationMax;
    }

    public double getMaxSpindleSpeed() {
        return maxSpindleSpeed;
    }

    public void setMaxSpindleSpeed(double maxSpindleSpeed) {
        this.maxSpindleSpeed = maxSpindleSpeed;
    }

    public double getNormalFeedRate() {
        return normalFeedRate;
    }

    public void setNormalFeedRate(double normalFeedRate) {
        this.normalFeedRate = normalFeedRate;
    }

    public int getBaseCycleTimeSec() {
        return baseCycleTimeSec;
    }

    public void setBaseCycleTimeSec(int baseCycleTimeSec) {
        this.baseCycleTimeSec = baseCycleTimeSec;
    }

    public double getToolWearRatePerCycle() {
        return toolWearRatePerCycle;
    }

    public void setToolWearRatePerCycle(double toolWearRatePerCycle) {
        this.toolWearRatePerCycle = toolWearRatePerCycle;
    }

    public double getMaintenanceDecayRate() {
        return maintenanceDecayRate;
    }

    public void setMaintenanceDecayRate(double maintenanceDecayRate) {
        this.maintenanceDecayRate = maintenanceDecayRate;
    }
}

