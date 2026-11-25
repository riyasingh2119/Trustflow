package com.trustflow.risk;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;

/** Loads weights and thresholds from risk_config with a short cache so edits reach every replica quickly. */
@Service
public class RiskConfigService {

    private static final Duration TTL = Duration.ofSeconds(30);

    private final RiskConfigRepository repo;
    private volatile Instant loadedAt = Instant.EPOCH;
    private volatile RiskParams params = RiskParams.defaults();

    public RiskConfigService(RiskConfigRepository repo) {
        this.repo = repo;
    }

    public RiskParams params() {
        if (Instant.now().isAfter(loadedAt.plus(TTL))) refresh();
        return params;
    }

    public RiskEngine engine() {
        return new RiskEngine(params());
    }

    public Map<String, Integer> all() {
        Map<String, Integer> out = new TreeMap<>();
        repo.findAll().forEach(c -> out.put(c.getKey(), c.getValue()));
        return out;
    }

    @Transactional
    public Map<String, Integer> update(Map<String, Integer> changes) {
        for (Map.Entry<String, Integer> e : changes.entrySet()) {
            RiskConfig c = repo.findById(e.getKey()).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown key: " + e.getKey()));
            if (e.getValue() == null || e.getValue() < 0 || e.getValue() > 100) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Value for " + e.getKey() + " must be 0-100");
            }
            c.setValue(e.getValue());
            repo.save(c);
        }
        Map<String, Integer> merged = all();
        if (merged.get("threshold.allow_max") >= merged.get("threshold.mfa_max")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "allow_max must be lower than mfa_max");
        }
        refresh();
        return merged;
    }

    public synchronized void refresh() {
        params = RiskParams.fromMap(all());
        loadedAt = Instant.now();
    }
}
