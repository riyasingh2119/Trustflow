package com.trustflow.gateway;

import com.trustflow.policy.PolicyAction;

public record AccessCommand(Long resourceId, PolicyAction action, Integer simulatedHour,
                            String deviceId, String mfaToken, String ip) { }
