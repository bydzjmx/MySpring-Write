package com.jmx.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * 自定义service实现
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface MyService {
    String value() default "";
}
