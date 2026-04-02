package com.rmsys.plcdemo.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rmsys.plcdemo.config.SimulatorProperties;
import com.rmsys.plcdemo.domain.model.NormalizedTelemetryDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class BackendTelemetryClient {
    private static final Logger log = LoggerFactory.getLogger(BackendTelemetryClient.class);
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI ingestUri;

    public BackendTelemetryClient(SimulatorProperties properties) {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
        this.ingestUri = URI.create(properties.getBackendBaseUrl() + properties.getIngestPath());
    }

    public SendResult send(NormalizedTelemetryDto payload) {
        String body;
        try {
            body = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return new SendResult(false, false, "Serialization failure: " + ex.getMessage(), null);
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(ingestUri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(5))
                    .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int code = response.statusCode();
                if (code >= 200 && code < 300) {
                    return new SendResult(true, false, "HTTP " + code, code);
                }
                String message = "HTTP " + code + " from ingest for " + payload.debugMachineCode();
                if (code >= 500 && attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                    continue;
                }
                boolean retryable = code >= 500;
                if (retryable) {
                    log.warn("{} after {} attempt(s)", message, attempt);
                } else {
                    log.warn("{} (non-retryable)", message);
                }
                return new SendResult(false, retryable, message, code);
            } catch (Exception ex) {
                if (attempt < MAX_ATTEMPTS) {
                    backoff(attempt);
                    continue;
                }
                String message = "Network failure for " + payload.debugMachineCode() + ": " + ex.getMessage();
                log.debug(message);
                return new SendResult(false, true, message, null);
            }
        }
        return new SendResult(false, true, "Unexpected send flow", null);
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(200L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    public record SendResult(boolean success, boolean retryable, String message, Integer statusCode) {
    }
}

