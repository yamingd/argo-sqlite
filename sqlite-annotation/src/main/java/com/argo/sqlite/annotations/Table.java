package com.argo.sqlite.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by user on 8/13/15.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE})
public @interface Table {

    /**
     * Table Name
     * @return String
     */
    String value() default "";

    /**
     * Database Tag
     * @return String
     */
    String context() default "default";
}
