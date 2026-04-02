package com.rmsys.plcdemo.config;

import com.rmsys.plcdemo.domain.model.MachineProfile;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {
    private String backendBaseUrl = "http://localhost:8080";
    private String ingestPath = "/api/v1/ingest/telemetry";
    private long tickMs = 1000;
    private String simulatorNode = "SIM-LOCAL-01";
    private boolean consoleEnabled = true;
    private boolean guiEnabled = false;
    private List<MachineProfile> machines = new ArrayList<>();

    public String getBackendBaseUrl() {
        return backendBaseUrl;
    }

    public void setBackendBaseUrl(String backendBaseUrl) {
        this.backendBaseUrl = backendBaseUrl;
    }

    public String getIngestPath() {
        return ingestPath;
    }

    public void setIngestPath(String ingestPath) {
        this.ingestPath = ingestPath;
    }

    public long getTickMs() {
        return tickMs;
    }

    public void setTickMs(long tickMs) {
        this.tickMs = tickMs;
    }

    public String getSimulatorNode() {
        return simulatorNode;
    }

    public void setSimulatorNode(String simulatorNode) {
        this.simulatorNode = simulatorNode;
    }

    public boolean isConsoleEnabled() {
        return consoleEnabled;
    }

    public void setConsoleEnabled(boolean consoleEnabled) {
        this.consoleEnabled = consoleEnabled;
    }

    public boolean isGuiEnabled() {
        return guiEnabled;
    }

    public void setGuiEnabled(boolean guiEnabled) {
        this.guiEnabled = guiEnabled;
    }

    public List<MachineProfile> getMachines() {
        return machines;
    }

    public void setMachines(List<MachineProfile> machines) {
        this.machines = machines;
    }
}

