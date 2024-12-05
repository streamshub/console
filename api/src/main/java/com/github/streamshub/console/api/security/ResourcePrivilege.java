package com.github.streamshub.console.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.streamshub.console.config.security.Privilege;

/**
 * Method annotation used by the {@link AuthorizationInterceptor} to declare
 * the privilege a principal must be granted to execute the annotated method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourcePrivilege {

    Privilege value() default Privilege.ALL;

}
