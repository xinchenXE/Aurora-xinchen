package com.aurora.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
//TODO 自定义注解
public @interface OptLog {

    String optType() default "";
}
