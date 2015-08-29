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
     * if this column is primary key
     * @return Boolean
     */
    boolean pk() default false;
    /**
     * Column's Name
     * @return String
     */
    String name() default "";

    /**
     * Generate database index for this column
     * @return boolean
     */
    boolean index() default false;
}
