package com.trustflow.audit;

import com.trustflow.risk.Decision;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** GET /api/audit (ADMIN, AUDITOR). Read-only on purpose: audit records are append-only. */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    public record Summary(Map<String, Long> byDecision, double averageScore, long total) { }

    private final AuditEventRepository repo;

    public AuditController(AuditEventRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public Page<AuditEvent> search(@RequestParam(required = false) String username,
                                   @RequestParam(required = false) Decision decision,
                                   @RequestParam(required = false) String resource,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "25") int size) {
        Specification<AuditEvent> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (username != null && !username.isBlank())
                ps.add(cb.equal(cb.lower(root.get("username")), username.toLowerCase()));
            if (decision != null) ps.add(cb.equal(root.get("decision"), decision));
            if (resource != null && !resource.isBlank())
                ps.add(cb.like(cb.lower(root.get("resourceName")), "%" + resource.toLowerCase() + "%"));
            return cb.and(ps.toArray(new Predicate[0]));
        };
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "timestamp"));
        return repo.findAll(spec, pr);
    }

    @GetMapping("/summary")
    public Summary summary() {
        Map<Decision, Long> counts = new EnumMap<>(Decision.class);
        for (Decision d : Decision.values()) counts.put(d, 0L);
        for (Object[] row : repo.countByDecision()) counts.put((Decision) row[0], (Long) row[1]);
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        counts.forEach((k, v) -> out.put(k.name(), v));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        Double avg = repo.averageScore();
        return new Summary(out, avg == null ? 0 : Math.round(avg * 10) / 10.0, total);
    }
}
