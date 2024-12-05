package com.github.streamshub.console.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

/**
 * Binding annotation to mark methods that should be intercepted by the
 * {@link AuthorizationInterceptor}.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Authorized {
}
