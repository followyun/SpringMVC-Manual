package com.my.annotation;

import java.lang.annotation.*;

/**
 */
@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyPathValue {
    String value();
}
