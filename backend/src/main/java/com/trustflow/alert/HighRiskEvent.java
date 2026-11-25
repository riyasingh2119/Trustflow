package com.trustflow.alert;

public record HighRiskEvent(String user, String resource, int score, String decision, String reason) { }
