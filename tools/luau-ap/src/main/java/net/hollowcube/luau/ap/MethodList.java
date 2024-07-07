package net.hollowcube.luau.ap;

import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record MethodList(@NotNull List<Method> methods) {

    record Arg(@NotNull String name, @NotNull TypeConverter type) {
    }

    record Method(
            @NotNull String name,
            @NotNull String methodName,
            @NotNull List<Arg> args,
            @Nullable TypeConverter ret
    ) {
    }

    public static @NotNull MethodList collect(@NotNull Messager log, @NotNull Map<TypeName, TypeConverter> typeConverters, @NotNull List<? extends Element> elems) {
        var methods = new ArrayList<Method>();
        for (var elem : elems) {
            if (!(elem instanceof ExecutableElement method)) continue;

            var methodName = method.getSimpleName().toString();
            var name = Names.toMethodName(methodName);

            var args = new ArrayList<Arg>();
            for (var arg : method.getParameters()) {
                var type = typeConverters.get(TypeName.get(arg.asType()));
                if (type == null) {
                    log.printError("Unsupported type: " + arg.asType(), arg);
                    continue;
                }
                args.add(new Arg(arg.getSimpleName().toString(), type));
            }

            TypeConverter ret = null;
            TypeName retType = TypeName.get(method.getReturnType());
            if (retType != TypeName.VOID) {
                ret = typeConverters.get(retType);
                if (ret == null) {
                    log.printError("Unsupported return type: " + retType, method);
                    continue;
                }
            }

            methods.add(new Method(name, methodName, args, ret));
        }
        return new MethodList(methods);
    }

    public boolean isEmpty() {
        return methods.isEmpty();
    }

}
