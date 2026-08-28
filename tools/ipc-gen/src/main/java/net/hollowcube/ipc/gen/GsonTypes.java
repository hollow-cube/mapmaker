package net.hollowcube.ipc.gen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;

/// Turns a compile-time [TypeMirror] into the `java.lang.reflect.Type` expression Gson needs at
/// runtime to (de)serialize a value of it.
///
/// Deliberately built out of `Foo.class` and [com.google.gson.reflect.TypeToken#getParameterized]
/// rather than an anonymous `new TypeToken<Foo<Bar>>() {}`: the anonymous form recovers its type
/// argument by reflecting on a generic supertype, which is exactly the shape native image has to be
/// told about, and `getParameterized` is a plain call over class literals that needs nothing.
final class GsonTypes {

    private static final ClassName TYPE_TOKEN = ClassName.get("com.google.gson.reflect", "TypeToken");

    /// The runtime type expression for `type`, or null having reported a diagnostic if it is one
    /// the wire format cannot carry (a type variable, a wildcard, or a type that did not resolve).
    static @Nullable CodeBlock runtimeType(Messager messager, Element site, TypeMirror type) {
        return switch (type.getKind()) {
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE ->
                CodeBlock.of("$T.class", TypeName.get(type).box());
            case ARRAY -> CodeBlock.of("$T.class", TypeName.get(type));
            case DECLARED -> declaredType(messager, site, (DeclaredType) type);
            default -> {
                messager.printError("ipc: cannot serialize '" + type + "'; ipc methods must use "
                    + "concrete types, not type variables or wildcards", site);
                yield null;
            }
        };
    }

    /// Whether [#runtimeType] produces a call rather than a class literal, and so is worth hoisting
    /// into a constant instead of being rebuilt on every request.
    static boolean isParameterized(TypeMirror type) {
        return type.getKind() == javax.lang.model.type.TypeKind.DECLARED
            && !((DeclaredType) type).getTypeArguments().isEmpty();
    }

    private static @Nullable CodeBlock declaredType(Messager messager, Element site, DeclaredType type) {
        var raw = ClassName.get((TypeElement) type.asElement());
        var arguments = type.getTypeArguments();
        if (arguments.isEmpty()) return CodeBlock.of("$T.class", raw);

        var argumentTypes = new ArrayList<CodeBlock>(arguments.size());
        for (var argument : arguments) {
            var argumentType = runtimeType(messager, site, argument);
            if (argumentType == null) return null;
            argumentTypes.add(argumentType);
        }
        return CodeBlock.of("$T.getParameterized($T.class, $L).getType()",
            TYPE_TOKEN, raw, CodeBlock.join(argumentTypes, ", "));
    }

    private GsonTypes() {
    }
}
