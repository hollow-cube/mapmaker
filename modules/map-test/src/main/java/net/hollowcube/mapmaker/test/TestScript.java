package net.hollowcube.mapmaker.test;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(TestScripts.class)
public @interface TestScript {
    String path() default "/main.luau";

    @Language("Luau")
    String value();
}
