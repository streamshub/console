package com.github.streamshub.console.api.v1alpha1.spec.security;

import java.util.List;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public abstract class Security {

    private List<Subject> subjects;
    private List<Role> roles;
    private List<Audit> audit;

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public List<Audit> getAudit() {
        return audit;
    }

    public void setAudit(List<Audit> audit) {
        this.audit = audit;
    }

}
