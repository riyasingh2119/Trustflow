package com.trustflow.alert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trustflow.common.TrustFlowMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Posts a small JSON message to the API Gateway endpoint, which invokes Lambda, which publishes to SNS. */
@Component
public class AlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(AlertPublisher.class);

    private final String url;
    private final String key;
    private final ObjectMapper mapper;
    private final TrustFlowMetrics metrics;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    public AlertPublisher(@Value("${trustflow.alert.url:}") String url,
                          @Value("${trustflow.alert.key:}") String key,
                          ObjectMapper mapper, TrustFlowMetrics metrics) {
        this.url = url;
        this.key = key;
        this.mapper = mapper;
        this.metrics = metrics;
    }

    @Async
    @EventListener
    public void onHighRisk(HighRiskEvent e) {
        log.warn("HIGH RISK DENY user={} resource={} score={} reason={}", e.user(), e.resource(), e.score(), e.reason());
        if (url == null || url.isBlank()) {
            log.info("ALERT_URL not configured; skipping API Gateway call");
            return;
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "user", e.user(), "resource", e.resource(), "score", e.score(),
                    "decision", e.decision(), "reason", e.reason()));
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (key != null && !key.isBlank()) rb.header("X-TrustFlow-Key", key);   // shared secret checked by the Lambda
            HttpRequest req = rb.build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 == 2) {
                metrics.highRiskAlert();
            } else {
                log.error("Alert endpoint returned HTTP {}", res.statusCode());
            }
        } catch (Exception ex) {
            // An alert failure must never break the access decision path.
            log.error("Could not publish high-risk alert: {}", ex.toString());
        }
    }
}
