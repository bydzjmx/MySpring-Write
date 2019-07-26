package com.jmx.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * 自定义Autowired实现
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface MyAutowired {
    String value() default "";
}
