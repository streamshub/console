package com.github.streamshub.console.api.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.streamshub.console.config.security.Privilege;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourcePrivilege {

    Privilege action() default Privilege.ALL;

}
