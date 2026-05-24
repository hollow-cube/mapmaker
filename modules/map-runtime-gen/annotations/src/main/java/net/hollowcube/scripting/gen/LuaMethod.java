package net.hollowcube.scripting.gen;

import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface LuaMethod {

    @MagicConstant(stringValues = {
        "__add", "__sub", "__mul", "__div",
        "__unm", "__mod", "__pow", "__idiv",
        "__concat", "__eq", "__le", "__lt",
        "__len", "__tostring",
        "__iter", "__call",

        // These have special handling already, could add fallbacks to an override
        // on the type but not implementing for now as there is no existing use case.
        // "__namecall", "__index", "__newindex",
    })
    String meta() default "";

}
