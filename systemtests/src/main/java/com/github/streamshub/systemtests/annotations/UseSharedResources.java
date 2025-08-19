package com.github.streamshub.systemtests.annotations;

import com.github.streamshub.systemtests.interfaces.UseSharedResourcesExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(UseSharedResourcesExtension.class)
public @interface UseSharedResources {
    String value();
}
