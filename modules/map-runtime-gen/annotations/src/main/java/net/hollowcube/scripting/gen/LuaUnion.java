package net.hollowcube.scripting.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Applied alongside [LuaExport] on an abstract sealed class to declare a discriminated-union
/// type. The class's permitted subclasses become the union variants; the parent's declared
/// [LuaProperty] / [LuaMethod] members are emitted as a non-exported common shape that each
/// variant intersects with.
///
/// The emitted Luau is:
/// ```luau
/// type __NameCommon = { ...common members }
/// export type Variant1 = __NameCommon & { ...variant1 members }
/// export type Variant2 = __NameCommon & { ...variant2 members }
/// export type Name = Variant1 | Variant2
/// ```
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LuaUnion {

    /// When set to a non-empty value, every permitted variant must declare a [LuaProperty]
    /// with this name whose return type is a string literal (e.g. `@luaReturn "block"`).
    /// Slopgen enforces this contract and additionally requires the literal values to be
    /// pairwise distinct across the family.
    ///
    /// When left empty (the default) the union has no discriminator contract — variants are
    /// still merged into `Name = V1 | V2`, but scripts cannot narrow via a tag check.
    String discriminator() default "";

}
