package net.hollowcube.canvas.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Action {
    String value();

    /**
     * If set, the action function will be called in a virtual thread.
     */
    boolean async() default false;
}
