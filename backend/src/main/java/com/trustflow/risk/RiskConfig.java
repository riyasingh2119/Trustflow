package com.trustflow.risk;

import jakarta.persistence.*;

@Entity
@Table(name = "risk_config")
public class RiskConfig {
    @Id
    @Column(name = "key")
    private String key;
    @Column(name = "value", nullable = false)
    private int value;

    protected RiskConfig() { }
    public String getKey() { return key; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
}
