package net.hollowcube.mapmaker.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Container annotation for repeating [TestScript]. Use the repeated form
/// directly - this type is rarely referenced by hand.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TestScripts {
    TestScript[] value();
}
