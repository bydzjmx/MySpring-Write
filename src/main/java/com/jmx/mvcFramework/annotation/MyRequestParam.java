package com.jmx.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * 自定义RequestParam实现
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface MyRequestParam {
    String value() default "";
}
