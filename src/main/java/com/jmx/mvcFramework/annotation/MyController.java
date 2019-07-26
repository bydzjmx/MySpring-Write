package com.jmx.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * 自定义controller注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyController {
    String value() default "";
}
