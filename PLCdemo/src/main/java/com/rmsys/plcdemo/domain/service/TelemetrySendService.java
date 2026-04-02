package com.rmsys.plcdemo.domain.service;

import com.rmsys.plcdemo.domain.model.ConnectionStatus;
import com.rmsys.plcdemo.domain.model.MachineRuntimeState;
import com.rmsys.plcdemo.domain.model.MachineScenario;
import com.rmsys.plcdemo.domain.model.NormalizedTelemetryDto;
import com.rmsys.plcdemo.infrastructure.http.BackendTelemetryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;

@Service
public class TelemetrySendService {

    private static final Logger log = LoggerFactory.getLogger(TelemetrySendService.class);

    private final BackendTelemetryClient client;

    public TelemetrySendService(BackendTelemetryClient client) {
        this.client = client;
    }

    public void handleSend(MachineRuntimeState state, Deque<NormalizedTelemetryDto> queue) {
        if (state.isSendPaused() || state.getConnectionStatus() == ConnectionStatus.OFFLINE) {
            return;
        }

        int interval = Math.max(1, state.getSlowSendIntervalSec());
        state.setTickCounter(state.getTickCounter() + 1);
        if (state.getTickCounter() < interval) {
            return;
        }
        state.setTickCounter(0);

        NormalizedTelemetryDto oldest = queue.pollFirst();
        if (oldest == null) {
            return;
        }

        Instant now = Instant.now();
        BackendTelemetryClient.SendResult result = client.send(oldest);
        state.setLastSentTs(now);

        if (result.success()) {
            state.setLastSuccessTs(now);
            state.setLastErrorMessage(null);
            if (state.getCurrentScenario() == MachineScenario.DUPLICATE) {
                if (!state.isDuplicatePending()) {
                    state.setDuplicatePending(true);
                    queue.addFirst(oldest);
                    log.debug("Duplicate mode: re-enqueuing packet for {}", oldest.debugMachineCode());
                } else {
                    state.setDuplicatePending(false);
                }
            } else {
                state.setDuplicatePending(false);
            }
        } else {
            state.setLastErrorMessage(result.message());
            if (result.retryable() && queue.size() < 100) {
                queue.addFirst(oldest);
                log.debug("Retrying packet for {} on next tick: {}", oldest.debugMachineCode(), result.message());
            } else if (result.retryable()) {
                log.warn("Drop packet for {} - queue full after send failure", oldest.debugMachineCode());
            } else {
                log.warn("Dropping non-retryable packet for {}: {}", oldest.debugMachineCode(), result.message());
            }
        }
    }
}
