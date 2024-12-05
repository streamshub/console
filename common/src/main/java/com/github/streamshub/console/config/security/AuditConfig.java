package com.github.streamshub.console.config.security;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class AuditConfig extends RuleConfig {

    Audit decision;

    public Audit getDecision() {
        return decision;
    }

    public void setDecision(Audit decision) {
        this.decision = decision;
    }
}
