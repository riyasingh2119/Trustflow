package com.trustflow.gateway;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.trustflow.risk.Decision;
import com.trustflow.risk.FactorBreakdown;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccessResponse(Decision decision, int score, FactorBreakdown factors, String reason,
                             String challengeId, boolean mfaSatisfied, String resource, String sensitivity,
                             String deviceStatus, int hour, Long auditId) { }
