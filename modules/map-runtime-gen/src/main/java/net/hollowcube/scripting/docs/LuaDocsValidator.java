package net.hollowcube.scripting.docs;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/// Enforces the shape of a [Docs] block. The Luau type expressions are not parsed
/// here, that happens later in the docs module. We only care about which tags are present
/// and how many.
public final class LuaDocsValidator {

    /// Decides whether malformed docs are an error. Will flip to true after existing code is migrated.
    public static final boolean STRICT = false;

    private static final Diagnostic.Kind SEVERITY =
        STRICT ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;

    private final Messager messager;

    public LuaDocsValidator(Messager messager) {
        this.messager = messager;
    }

    /// Methods must have one or more declared returns if non-void. Void may not declare any returns.
    public void validateMethod(Element element, Docs docs, boolean javaVoid) {
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

    /// Property getters must have one declared return and zero declared params.
    public void validateGetter(Element element, Docs docs) {
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

    /// Property setters must have one declared param and zero declared returns.
    public void validateSetter(Element element, Docs docs) {
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

    /// Meta methods must have exact params declared for its shape.
    /// TODO: per-method shape validation.
    public void validateMeta(Element element, Docs docs, boolean javaVoid) {
        validateMethod(element, docs, javaVoid);
    }

    /// TODO: not sure this works, a setter may be less strict than a getter (eg set AnyText, get Text)
    public void validatePropertyConsistency(Element setterElement, Docs getter, Docs setter) {
        if (getter.returns().isEmpty() || setter.params().isEmpty()) return;
        var getType = getter.returns().getFirst().typeExpr();
        var setType = setter.params().getFirst().typeExpr();
        if (!getType.equals(setType)) {
            messager.printMessage(SEVERITY,
                "Property getter @luaReturn and setter @luaParam must declare the same type ("
                + "got '" + getType + "' vs '" + setType + "')",
                setterElement);
        }
    }

    public void validateLibraryContainer(Element element, Docs docs) {
        reportTagDiagnostics(element, docs);
        rejectTag(element, !docs.params().isEmpty(),
            "@luaParam is not valid on a library — declare it on a method or accessor");
        rejectTag(element, !docs.returns().isEmpty(),
            "@luaReturn is not valid on a library — declare it on a method or accessor");
        rejectTag(element, !docs.generics().isEmpty(),
            "@luaGeneric is not valid on a library — declare it on the method");
    }

    public void validateExportContainer(Element element, Docs docs) {
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

    private void reportTagDiagnostics(Element element, Docs docs) {
        for (var d : docs.diagnostics()) {
            messager.printMessage(SEVERITY, d.message(), element);
        }
    }
}
