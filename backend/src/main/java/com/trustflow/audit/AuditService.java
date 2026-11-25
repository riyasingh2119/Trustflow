package com.trustflow.audit;

import com.trustflow.context.DeviceStatus;
import com.trustflow.policy.PolicyAction;
import com.trustflow.risk.Decision;
import com.trustflow.risk.FactorBreakdown;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditEventRepository repo;

    public AuditService(AuditEventRepository repo) {
        this.repo = repo;
    }

    public AuditEvent record(String username, Long resourceId, String resourceName, PolicyAction action,
                             int score, FactorBreakdown f, Decision decision, DeviceStatus device,
                             String ip, String reason) {
        FactorBreakdown fb = f == null ? new FactorBreakdown(0, 0, 0, 0) : f;
        return repo.save(new AuditEvent(username, resourceId, resourceName, action, score,
                fb.device(), fb.resource(), fb.time(), fb.auth(), decision, device, ip, truncate(reason)));
    }

    private static String truncate(String s) {
        return s != null && s.length() > 480 ? s.substring(0, 480) : s;
    }
}
