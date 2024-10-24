package com.github.streamshub.console.config.security;

import java.util.ArrayList;
import java.util.List;

public class RoleConfig {

    private String name;
    private List<RuleConfig> rules = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RuleConfig> getRules() {
        return rules;
    }

    public void setRules(List<RuleConfig> rules) {
        this.rules = rules;
    }

}
