package com.trustflow.risk;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** GET, PUT /api/admin/risk-config (ADMIN only). */
@RestController
@RequestMapping("/api/admin/risk-config")
public class RiskConfigController {

    private final RiskConfigService service;

    public RiskConfigController(RiskConfigService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Integer> get() {
        return service.all();
    }

    @PutMapping
    public Map<String, Integer> put(@RequestBody Map<String, Integer> changes) {
        return service.update(changes);
    }
}
