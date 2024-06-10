package com.github.streamshub.console.config.security;

import java.util.ArrayList;
import java.util.List;

public class SecurityConfig {

    private List<SubjectConfig> subjects = new ArrayList<>();
    private List<RoleConfig> roles = new ArrayList<>();

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

}
