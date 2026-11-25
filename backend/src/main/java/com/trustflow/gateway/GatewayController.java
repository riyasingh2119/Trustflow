package com.trustflow.gateway;

import com.trustflow.common.ClientIp;
import com.trustflow.policy.PolicyAction;
import com.trustflow.risk.Decision;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gateway")
public class GatewayController {

    /** simulatedHour is honoured only when trustflow.demo-mode is true. */
    public record AccessBody(@NotNull Long resourceId, @NotNull PolicyAction action,
                             @Min(0) @Max(23) Integer simulatedHour) { }

    private final AccessDecisionFilter gateway;

    public GatewayController(AccessDecisionFilter gateway) {
        this.gateway = gateway;
    }

    /** ALLOW and MFA_REQUIRED return 200 with the decision in the body; DENY returns 403 with the same body. */
    @PostMapping("/access")
    public ResponseEntity<AccessResponse> access(Authentication auth,
                                                 @Valid @RequestBody AccessBody body,
                                                 @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
                                                 @RequestHeader(value = "X-MFA-Token", required = false) String mfaToken,
                                                 HttpServletRequest req) {
        AccessResponse out = gateway.evaluate(auth.getName(),
                new AccessCommand(body.resourceId(), body.action(), body.simulatedHour(), deviceId, mfaToken, ClientIp.of(req)));
        HttpStatus status = out.decision() == Decision.DENY ? HttpStatus.FORBIDDEN : HttpStatus.OK;
        return ResponseEntity.status(status).body(out);
    }
}
