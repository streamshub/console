package com.github.streamshub.systemtests.annotations;

import com.github.streamshub.systemtests.interfaces.TestBucketExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestBucketExtension.class)
public @interface TestBucket {
    String value();
}
