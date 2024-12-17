package com.github.streamshub.console.api.v1alpha1.spec.security;

import java.util.List;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class Role {

    @Required
    private String name;

    private List<Rule> rules;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

}
