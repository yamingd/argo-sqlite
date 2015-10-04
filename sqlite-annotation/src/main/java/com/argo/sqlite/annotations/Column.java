package com.argo.sqlite.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by user on 8/13/15.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD})
public @interface Column {

    /**
     * @return boolean
     */
    boolean pk() default false;
    /**
     *
     * @return String
     */
    String name() default "";

    /**
     * @return boolean
     */
    boolean index() default false;
}
