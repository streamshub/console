package com.github.streamshub.console.config.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class RoleConfig {

    @NotBlank
    private String name;

    @Valid
    @NotEmpty
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
