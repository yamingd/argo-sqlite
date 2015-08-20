package com.argo.sqlite.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by user on 8/14/15.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.FIELD})
public @interface RefLink {

    /**
     * Ref Column
     * @return
     */
    String on() default "";
}
