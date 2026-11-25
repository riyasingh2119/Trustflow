package com.trustflow.risk;

public record RiskResult(int score, FactorBreakdown factors, Decision decision, String reason) { }
