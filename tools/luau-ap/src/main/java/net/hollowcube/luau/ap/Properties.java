package net.hollowcube.luau.ap;

import net.hollowcube.luau.ap.tree.Node;
import net.hollowcube.luau.ap.util.DocContent;
import net.hollowcube.luau.ap.util.LuaTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Properties {

    public static @NotNull List<Node.Property> collect(@NotNull Messager log, @NotNull Elements elements, @NotNull LuaTypeRegistry types, @NotNull List<? extends Element> elems) {
        var props = new ArrayList<Node.Property>();
        for (var elem : elems) {
            switch (elem) {
                case ExecutableElement method -> {
                    var prop = collectMethod(log, elements, types, method);
                    if (prop != null) props.add(prop);
                }
                case VariableElement variable when variable.getKind().isField() -> {
                    var prop = collectField(log, elements, types, variable);
                    if (prop != null) props.add(prop);
                }
                default -> log.printError("Only fields and methods may be @LuaProperty", elem);
            }
        }
        return props;
    }

    private static @Nullable Node.Property collectMethod(@NotNull Messager log, @NotNull Elements elements, @NotNull LuaTypeRegistry types, @NotNull ExecutableElement method) {
        var methodName = method.getSimpleName().toString();
        var name = toPropertyName(methodName);
        final Set<Modifier> mods = method.getModifiers();

        var returnType = types.forTypeMirror(method.getReturnType());
        if (returnType == null) {
            log.printError("Unsupported return type: " + method.getReturnType(), method);
            return null;
        }

        var doc = DocContent.parse(elements.getDocComment(method));

        return new Node.Property(method, name, methodName + "()", mods.contains(Modifier.STATIC), returnType, doc);
    }

    private static @Nullable Node.Property collectField(@NotNull Messager log, @NotNull Elements elements, @NotNull LuaTypeRegistry types, @NotNull VariableElement field) {
        final Set<Modifier> mods = field.getModifiers();
        if (!mods.contains(Modifier.PUBLIC) || mods.contains(Modifier.STATIC)) {
            log.printError("@LuaProperty fields must be public and non-static", field);
            return null;
        }

        var fieldName = field.getSimpleName().toString();
        var name = toPropertyName(fieldName);

        var fieldType = types.forTypeMirror(field.asType());
        if (fieldType == null) {
            log.printError("Unsupported field type: " + field.asType(), field);
            return null;
        }

        var doc = DocContent.parse(elements.getDocComment(field));

        return new Node.Property(field, name, fieldName, false, fieldType, doc);
    }

    private static @NotNull String toPropertyName(@NotNull String methodName) {
        if (methodName.startsWith("get")) methodName = methodName.substring(3);
        return Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
    }

}
