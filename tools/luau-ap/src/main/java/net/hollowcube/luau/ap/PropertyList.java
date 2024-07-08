package net.hollowcube.luau.ap;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PropertyList(@NotNull List<Property> properties) {

    public record Property(@NotNull String name, @NotNull String accessor, boolean isPin,
                           @UnknownNullability TypeConverter type) {
    }

    public static @NotNull PropertyList collect(@NotNull Messager log, @NotNull Map<TypeName, TypeConverter> typeConverters, @NotNull List<? extends Element> elems) {
        var props = new ArrayList<Property>();
        for (var elem : elems) {
            switch (elem) {
                case ExecutableElement method -> {
                    var prop = collectMethod(log, typeConverters, method);
                    if (prop != null) props.add(prop);
                }
                case VariableElement variable when variable.getKind().isField() -> {
                    var prop = collectField(log, typeConverters, variable);
                    if (prop != null) props.add(prop);
                }
                default -> log.printError("Only fields and methods may be @LuaProperty", elem);
            }
        }
        return new PropertyList(props);
    }

    private static @Nullable Property collectMethod(@NotNull Messager log, @NotNull Map<TypeName, TypeConverter> typeConverters, @NotNull ExecutableElement method) {
        var methodName = method.getSimpleName().toString();
        var name = Names.toPropertyName(methodName);

        var returnType = TypeName.get(method.getReturnType());
        var simpleConverter = typeConverters.get(returnType);
        if (simpleConverter != null) {
            return new Property(name, methodName + "()", false, simpleConverter);
        }

        if (returnType instanceof ParameterizedTypeName paramType && paramType.rawType.equals(Types.PIN)) {
            return new Property(name, methodName + "()", true, null);
        }

        log.printError("Unsupported return type: " + returnType, method);
        return null;
    }

    private static @Nullable Property collectField(@NotNull Messager log, @NotNull Map<TypeName, TypeConverter> typeConverters, @NotNull VariableElement field) {
        final Set<Modifier> mods = field.getModifiers();
        if (!mods.contains(Modifier.PUBLIC) || mods.contains(Modifier.STATIC)) {
            log.printError("@LuaProperty fields must be public and non-static", field);
            return null;
        }

        var fieldName = field.getSimpleName().toString();
        var name = Names.toPropertyName(fieldName);

        var fieldType = TypeName.get(field.asType());
        var simpleConverter = typeConverters.get(fieldType);
        if (simpleConverter != null) {
            return new Property(name, fieldName, false, simpleConverter);
        }

        if (fieldType instanceof ParameterizedTypeName paramType && paramType.rawType.equals(Types.PIN)) {
            return new Property(name, fieldName, true, null);
        }

        log.printError("Unsupported field type: " + fieldType, field);
        return null;
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

}
