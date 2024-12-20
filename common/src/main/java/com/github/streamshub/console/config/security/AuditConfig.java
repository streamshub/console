package com.github.streamshub.console.config.security;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class AuditConfig extends RuleConfig {

    Decision decision;

    public Decision getDecision() {
        return decision;
    }

    public void setDecision(Decision decision) {
        this.decision = decision;
    }
}
