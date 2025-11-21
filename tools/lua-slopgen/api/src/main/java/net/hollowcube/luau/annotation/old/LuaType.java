package net.hollowcube.luau.annotation.old;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface LuaType {

    Class<?> implFor() default Object.class;

    String name() default "";

}
