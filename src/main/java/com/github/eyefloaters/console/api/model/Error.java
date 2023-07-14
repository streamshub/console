package com.github.eyefloaters.console.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Error {

    String kind = "Error";
    String id;
    String status;
    String code;
    String title;
    String detail;
    ErrorSource source;
    @JsonIgnore
    Throwable cause;

    public Error(String title, String detail, Throwable cause) {
        this.title = title;
        this.detail = detail;
        this.cause = cause;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public ErrorSource getSource() {
        return source;
    }

    public void setSource(ErrorSource source) {
        this.source = source;
    }

    public Throwable getCause() {
        return cause;
    }
}
