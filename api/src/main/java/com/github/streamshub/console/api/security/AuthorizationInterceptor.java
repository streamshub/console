package com.github.streamshub.console.api.security;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;

import io.quarkus.security.identity.SecurityIdentity;

@Authorized
@Priority(1)
@Interceptor
@Dependent
public class AuthorizationInterceptor {

    @Inject
    Logger logger;

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    UriInfo requestUri;

    @AroundInvoke
    Object authorize(InvocationContext context) throws Exception {
        ResourcePrivilege authz = context.getMethod().getAnnotation(ResourcePrivilege.class);
        String resourcePath = requestUri.getPath().substring("/api/".length());
        var requiredPermission = new ConsolePermission(resourcePath, authz.action());
        boolean allow = securityIdentity.checkPermission(requiredPermission)
                .subscribeAsCompletionStage()
                .get();

        if (!allow) {
            throw new ForbiddenException("Access denied");
        }

        return context.proceed();
    }

}
