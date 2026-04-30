package net.hollowcube.luau.slopgen.docs;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/// Enforces the slopgen-level shape of a [MemberDocs] block. The Luau type expressions
/// themselves are not parsed here — that happens later in the docs module. The rules here
/// only care about *which* tags are present and how many.
public final class LuaDocsValidator {

    /// Flip to `true` once every `@LuaLibrary` in the codebase has been migrated to declare
    /// `@luaParam` / `@luaReturn` / `@luaGeneric` tags. While `false`, validator findings are
    /// emitted as warnings instead of errors so the build keeps compiling during the
    /// migration period.
    public static final boolean STRICT = false;

    private static final Diagnostic.Kind SEVERITY =
        STRICT ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;

    private final Messager messager;

    public LuaDocsValidator(Messager messager) {
        this.messager = messager;
    }

    /// Validate a `@LuaMethod`. Non-void Java methods require ≥1 `@luaReturn`; void methods
    /// must have none.
    public void validateMethod(Element element, MemberDocs docs, boolean javaVoid) {
        reportTagDiagnostics(element, docs);
        if (javaVoid && !docs.returns().isEmpty()) {
            messager.printMessage(SEVERITY,
                "Void @LuaMethod must not declare @luaReturn", element);
        } else if (!javaVoid && docs.returns().isEmpty()) {
            messager.printMessage(SEVERITY,
                "@LuaMethod must declare at least one @luaReturn (use multiple tags for multi-return)",
                element);
        }
    }

    /// Validate a `@LuaProperty` getter (Java method whose name does not start with `set`).
    /// Requires exactly one `@luaReturn` and zero `@luaParam`.
    public void validateGetter(Element element, MemberDocs docs) {
        reportTagDiagnostics(element, docs);
        if (docs.returns().size() != 1) {
            messager.printMessage(SEVERITY,
                "@LuaProperty getter must declare exactly one @luaReturn", element);
        }
        if (!docs.params().isEmpty()) {
            messager.printMessage(SEVERITY,
                "@LuaProperty getter must not declare @luaParam", element);
        }
    }

    /// Validate a `@LuaProperty` setter. Requires exactly one `@luaParam` and zero
    /// `@luaReturn`.
    public void validateSetter(Element element, MemberDocs docs) {
        reportTagDiagnostics(element, docs);
        if (docs.params().size() != 1) {
            messager.printMessage(SEVERITY,
                "@LuaProperty setter must declare exactly one @luaParam", element);
        }
        if (!docs.returns().isEmpty()) {
            messager.printMessage(SEVERITY,
                "@LuaProperty setter must not declare @luaReturn", element);
        }
    }

    /// Validate a meta `@LuaMethod` (one with `meta != Meta.NONE`). Same as a regular method
    /// for return-presence purposes — Lua metamethods may yield zero or one value, and the
    /// existing emitter already handles void via `Model.MetaMethod.isVoid()`. Future work:
    /// model meta param shape.
    public void validateMeta(Element element, MemberDocs docs, boolean javaVoid) {
        validateMethod(element, docs, javaVoid);
    }

    /// Validate that a getter and a setter for the same property declare matching types
    /// (raw-string equality at this stage). The docs module re-validates structurally after
    /// parsing both sides as Luau type expressions.
    public void validatePropertyConsistency(Element setterElement, MemberDocs getter, MemberDocs setter) {
        if (getter.returns().isEmpty() || setter.params().isEmpty()) return;
        var getType = getter.returns().get(0).typeExpr();
        var setType = setter.params().get(0).typeExpr();
        if (!getType.equals(setType)) {
            messager.printMessage(SEVERITY,
                "Property getter @luaReturn and setter @luaParam must declare the same type ("
                + "got '" + getType + "' vs '" + setType + "')",
                setterElement);
        }
    }

    /// Validate library-level docs. Libraries are static-only and have no shared
    /// generics across methods, so `@luaParam` / `@luaReturn` / `@luaGeneric` are all
    /// rejected here — they belong on a method or accessor.
    public void validateLibraryContainer(Element element, MemberDocs docs) {
        reportTagDiagnostics(element, docs);
        rejectTag(element, !docs.params().isEmpty(),
            "@luaParam is not valid on a library — declare it on a method or accessor");
        rejectTag(element, !docs.returns().isEmpty(),
            "@luaReturn is not valid on a library — declare it on a method or accessor");
        rejectTag(element, !docs.generics().isEmpty(),
            "@luaGeneric is not valid on a library — declare it on the method");
    }

    /// Validate `@LuaExport` class-level docs. Type-level `@luaGeneric` declarations are
    /// allowed and become shared across every method, accessor, and meta-method on that
    /// type. Param/return tags are still rejected here — they belong on members.
    public void validateExportContainer(Element element, MemberDocs docs) {
        reportTagDiagnostics(element, docs);
        rejectTag(element, !docs.params().isEmpty(),
            "@luaParam is not valid on a @LuaExport class — declare it on a method or accessor");
        rejectTag(element, !docs.returns().isEmpty(),
            "@luaReturn is not valid on a @LuaExport class — declare it on a method or accessor");
        // @luaGeneric is allowed and is read by the model builder — no diagnostic here.
    }

    private void rejectTag(Element element, boolean condition, String message) {
        if (condition) messager.printMessage(SEVERITY, message, element);
    }

    private void reportTagDiagnostics(Element element, MemberDocs docs) {
        for (var d : docs.diagnostics()) {
            messager.printMessage(SEVERITY, d.message(), element);
        }
    }
}
