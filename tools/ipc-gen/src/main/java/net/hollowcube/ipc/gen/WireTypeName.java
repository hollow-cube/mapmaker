package net.hollowcube.ipc.gen;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

/// How a type is spelled in `wire.json`: qualified, except that `java.lang` and `java.util` are
/// dropped because `List<String>` diffs better than the same thing four times as long. Two types
/// that spell the same are the same on the wire, which is all the descriptor needs of a name.
final class WireTypeName {

    private static final List<String> ELIDED_PACKAGES = List.of("java.lang.", "java.util.");

    static String render(TypeMirror type) {
        return switch (type.getKind()) {
            case ARRAY -> render(((ArrayType) type).getComponentType()) + "[]";
            case DECLARED -> {
                var declared = (DeclaredType) type;
                var name = name((TypeElement) declared.asElement());
                if (declared.getTypeArguments().isEmpty()) yield name;
                yield name + declared.getTypeArguments().stream()
                    .map(WireTypeName::render)
                    .collect(Collectors.joining(", ", "<", ">"));
            }
            default -> type.toString();
        };
    }

    static String name(TypeElement element) {
        var qualified = element.getQualifiedName().toString();
        for (var prefix : ELIDED_PACKAGES) {
            if (qualified.startsWith(prefix) && qualified.indexOf('.', prefix.length()) < 0) {
                return qualified.substring(prefix.length());
            }
        }
        return qualified;
    }

    private WireTypeName() {
    }
}
