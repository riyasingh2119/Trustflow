package com.trustflow.risk;

/** Individual risk contributions; the total is bounded at 100 (30 + 30 + 15 + 25). */
public record FactorBreakdown(int device, int resource, int time, int auth) {
    public int total() {
        return device + resource + time + auth;
    }
}
