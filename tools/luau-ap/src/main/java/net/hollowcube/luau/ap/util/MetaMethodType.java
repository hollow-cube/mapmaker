package net.hollowcube.luau.ap.util;

import com.squareup.javapoet.TypeName;
import net.hollowcube.luau.ap.Types;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.List;

public enum MetaMethodType {
    // In these definitions there are a few special types:
    // MM_USER_TYPE: The implementing class type
    // MM_ANY: Any viable lua type (overloadable if in a parameter)

    INDEX(List.of(Types.STRING), Types.MM_ANY),
    NEWINDEX(List.of(Types.STRING, Types.MM_ANY), Types.MM_USER_TYPE),
    CALL(null, null), // Must be implemented as direct for now.
    TOSTRING(List.of(), Types.STRING),

    ADD(List.of(Types.MM_ANY), Types.MM_USER_TYPE),
    SUB(List.of(Types.MM_ANY), Types.MM_USER_TYPE),

    EQ(List.of(Types.MM_ANY), TypeName.BOOLEAN),
    ;

    private final List<TypeName> argTypes;
    private final TypeName returnType;

    MetaMethodType(@Nullable List<TypeName> argTypes, @Nullable TypeName returnType) {
        this.argTypes = argTypes;
        this.returnType = returnType;
    }

    public boolean requiresDirect() {
        return argTypes == null || returnType == null;
    }

    public boolean validate(@NotNull Messager log, @NotNull TypeName userType, @NotNull ExecutableElement method) {
        if (argTypes == null || returnType == null) return true;

        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
        var params = method.getParameters();
        int paramCount = isStatic ? params.size() - 1 : params.size();
        if (paramCount != argTypes.size()) {
            log.printError("Argument count mismatch, expected " + argTypes.size() + " but got " + params.size(), method);
            return false;
        }
        for (int i = 0; i < paramCount; i++) {
            TypeName expected = argTypes.get(i);
            TypeName actual = TypeName.get(params.get(i + (isStatic ? 1 : 0)).asType()).withoutAnnotations();
            if (!compareTypes(expected, actual, userType)) {
                log.printError("Argument type mismatch, expected " + expected + " but got " + actual, method);
                return false;
            }
        }

        TypeName actReturnType = TypeName.get(method.getReturnType()).withoutAnnotations();
        if (!compareTypes(returnType, actReturnType, userType)) {
            log.printError("Return type mismatch, expected " + returnType + " but got " + actReturnType, method);
            return false;
        }

        return true;
    }

    private boolean compareTypes(@NotNull TypeName expected, @NotNull TypeName actual, @NotNull TypeName userType) {
        if (expected.equals(Types.MM_ANY)) return true;
        if (expected.equals(Types.MM_USER_TYPE)) return actual.equals(userType);
        return expected.equals(actual);
    }
}
