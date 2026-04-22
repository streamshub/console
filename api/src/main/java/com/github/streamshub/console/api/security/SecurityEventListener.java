package com.github.streamshub.console.api.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.oidc.SecurityEvent;

@ApplicationScoped
public class SecurityEventListener {

    @Inject
    Logger logger;

    /**
     * Watch security (OIDC only) events and log for debugging.
     */
    public void event(@Observes SecurityEvent event) {
        var principalName = event.getSecurityIdentity().getPrincipal().getName();
        var eventType = event.getEventType();
        logger.debugf("OIDC event: principal=%s, event=%s", principalName, eventType);
    }
}
