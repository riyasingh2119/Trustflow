package com.trustflow.context;

public record AccessContext(DeviceStatus deviceStatus, int hour, int failedLogins, String ip) { }
