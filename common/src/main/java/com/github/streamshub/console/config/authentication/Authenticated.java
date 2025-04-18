package com.github.streamshub.console.config.authentication;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.streamshub.console.config.Trustable;

public interface Authenticated extends Trustable {

    AuthenticationConfig getAuthentication();

    void setAuthentication(AuthenticationConfig authentication);

    @JsonIgnore
    default Optional<Trustable> getTrustableAuthentication() {
        return Optional.ofNullable(getAuthentication())
                .map(AuthenticationConfig::getOidc);
    }
}
