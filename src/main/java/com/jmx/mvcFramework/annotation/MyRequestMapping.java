package com.jmx.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * 自定义RequestMapping实现
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface MyRequestMapping {
    String value() default "";
}
