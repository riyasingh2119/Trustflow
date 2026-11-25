package com.trustflow.common;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIp {
    private ClientIp() { }

    /** First hop of X-Forwarded-For when behind Nginx / ingress, otherwise the socket address. */
    public static String of(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
