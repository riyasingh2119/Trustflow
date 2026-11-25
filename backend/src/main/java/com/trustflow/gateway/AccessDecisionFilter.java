package com.trustflow.gateway;

import com.trustflow.alert.HighRiskEvent;
import com.trustflow.audit.AuditEvent;
import com.trustflow.audit.AuditService;
import com.trustflow.auth.JwtService;
import com.trustflow.common.TrustFlowMetrics;
import com.trustflow.context.AccessContext;
import com.trustflow.context.ContextCollector;
import com.trustflow.policy.PolicyEvaluator;
import com.trustflow.resource.Resource;
import com.trustflow.resource.ResourceRepository;
import com.trustflow.risk.Decision;
import com.trustflow.risk.FactorBreakdown;
import com.trustflow.risk.RiskConfigService;
import com.trustflow.risk.RiskResult;
import com.trustflow.user.Role;
import com.trustflow.user.User;
import com.trustflow.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * The resource gateway. For each request it runs the pipeline from the design document:
 * identity (already validated by JwtAuthFilter) -> user enabled -> resource lookup -> policy match ->
 * context -> risk score -> decision -> audit -> alert.
 */
@Component
public class AccessDecisionFilter {

    private final UserRepository users;
    private final ResourceRepository resources;
    private final PolicyEvaluator policies;
    private final ContextCollector contextCollector;
    private final RiskConfigService riskConfig;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final JwtService jwt;
    private final JdbcTemplate jdbc;
    private final TrustFlowMetrics metrics;

    public AccessDecisionFilter(UserRepository users, ResourceRepository resources, PolicyEvaluator policies,
                                ContextCollector contextCollector, RiskConfigService riskConfig, AuditService audit,
                                ApplicationEventPublisher events, JwtService jwt, JdbcTemplate jdbc,
                                TrustFlowMetrics metrics) {
        this.users = users;
        this.resources = resources;
        this.policies = policies;
        this.contextCollector = contextCollector;
        this.riskConfig = riskConfig;
        this.audit = audit;
        this.events = events;
        this.jwt = jwt;
        this.jdbc = jdbc;
        this.metrics = metrics;
    }

    public AccessResponse evaluate(String username, AccessCommand cmd) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        Resource res = resources.findById(cmd.resourceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found"));

        // 2. user disabled
        if (!user.isEnabled()) {
            return denyWithoutScore(user, res, cmd, "USER_DISABLED: the account is disabled");
        }

        // 3. RBAC / ABAC gate: no active, in-validity policy means DENY without needing a risk score
        if (!policies.isGranted(user, res.getId(), cmd.action(), Instant.now())) {
            String roles = user.getRoles().stream().map(Role::getName).sorted().toList().toString();
            return denyWithoutScore(user, res, cmd,
                    "NO_POLICY: no active policy grants " + roles + " " + cmd.action() + " on " + res.getName());
        }

        // 4-5. context and risk score
        AccessContext ctx = contextCollector.collect(user, cmd.deviceId(), cmd.simulatedHour(), cmd.ip());
        RiskResult risk = riskConfig.engine().evaluate(ctx.deviceStatus(), res.getSensitivity(), ctx.hour(), ctx.failedLogins());

        // 6. decision (with MFA step-up)
        Decision decision = risk.decision();
        String reason = risk.reason();
        String challenge = null;
        boolean mfaSatisfied = false;
        if (decision == Decision.MFA_REQUIRED) {
            if (cmd.mfaToken() != null && consumeMfaToken(cmd.mfaToken(), user, res, cmd)) {
                decision = Decision.ALLOW;
                mfaSatisfied = true;
                reason = reason + "; MFA verified, allowed for this single request";
            } else {
                challenge = jwt.challengeToken(user.getUsername(), res.getId(), cmd.action());
                metrics.mfa("issued");
                if (cmd.mfaToken() != null) reason = reason + "; supplied MFA token was not valid for this request";
            }
        }

        // 7. audit
        AuditEvent saved = audit.record(user.getUsername(), res.getId(), res.getName(), cmd.action(), risk.score(),
                risk.factors(), decision, ctx.deviceStatus(), cmd.ip(), reason);
        metrics.decision(decision.name());
        metrics.riskScore(risk.score());

        // 8. high-risk alert
        if (decision == Decision.DENY) {
            metrics.deniedResource(res.getName());
            events.publishEvent(new HighRiskEvent(user.getUsername(), res.getName(), risk.score(), decision.name(), reason));
        }

        return new AccessResponse(decision, risk.score(), risk.factors(), reason, challenge, mfaSatisfied,
                res.getName(), res.getSensitivity().name(), ctx.deviceStatus().name(), ctx.hour(), saved.getId());
    }

    private AccessResponse denyWithoutScore(User user, Resource res, AccessCommand cmd, String reason) {
        FactorBreakdown none = new FactorBreakdown(0, 0, 0, 0);
        AuditEvent saved = audit.record(user.getUsername(), res.getId(), res.getName(), cmd.action(), 0, none,
                Decision.DENY, null, cmd.ip(), reason);
        metrics.decision(Decision.DENY.name());
        metrics.deniedResource(res.getName());
        return new AccessResponse(Decision.DENY, 0, none, reason, null, false, res.getName(),
                res.getSensitivity().name(), null, -1, saved.getId());
    }

    /** A valid MFA token is bound to user, resource and action, and can be used exactly once. */
    private boolean consumeMfaToken(String token, User user, Resource res, AccessCommand cmd) {
        try {
            Claims c = jwt.parse(token);
            boolean bound = JwtService.MFA.equals(c.get("tt", String.class))
                    && user.getUsername().equals(c.getSubject())
                    && res.getId().equals(JwtService.longClaim(c, "rid"))
                    && cmd.action().name().equals(c.get("act", String.class));
            if (!bound) return false;
            int inserted = jdbc.update("INSERT INTO used_mfa_tokens (jti) VALUES (?) ON CONFLICT DO NOTHING", c.getId());
            return inserted == 1;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
