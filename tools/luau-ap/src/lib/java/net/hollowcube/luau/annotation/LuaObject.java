package net.hollowcube.luau.annotation;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LuaObject {

    @NotNull String name() default "";

    /**
     * If enabled, only static functions are allowed and a userdata type will not be generated.
     */
    boolean singleton() default false;

}
