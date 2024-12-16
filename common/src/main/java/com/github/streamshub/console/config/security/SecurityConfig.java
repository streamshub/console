package com.github.streamshub.console.config.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class SecurityConfig {

    @Valid
    private List<SubjectConfig> subjects = new ArrayList<>();

    @Valid
    private List<RoleConfig> roles = new ArrayList<>();

    @Valid
    private List<AuditConfig> audit = new ArrayList<>();

    public List<SubjectConfig> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<SubjectConfig> subjects) {
        this.subjects = subjects;
    }

    public List<RoleConfig> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleConfig> roles) {
        this.roles = roles;
    }

    public List<AuditConfig> getAudit() {
        return audit;
    }

    public void setAudit(List<AuditConfig> audit) {
        this.audit = audit;
    }

}
