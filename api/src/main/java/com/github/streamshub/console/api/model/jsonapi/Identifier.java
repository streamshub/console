package com.github.streamshub.console.api.model.jsonapi;

import java.util.Objects;

public record Identifier(String type, String id) {

    public boolean equals(String type, String id) {
        return Objects.equals(this.type, type) && Objects.equals(this.id, id);
    }
}
