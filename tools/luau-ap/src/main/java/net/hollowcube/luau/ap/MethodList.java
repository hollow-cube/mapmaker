package net.hollowcube.luau.ap;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import net.hollowcube.luau.ap.util.MetaMethodType;
import net.hollowcube.luau.ap.util.ProcUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record MethodList(@NotNull List<Method> methods) {

    public record Arg(@NotNull String name, @NotNull TypeConverter type) {
    }

    public record Method(
            @NotNull String name,
            @NotNull String methodName,
            // If present, args and ret are ignored.
            boolean isDirect,
            // If present, there is an implicit first arg of the type being implemented, unless its direct.
            boolean isStatic,
            // Method defined on wrapper class
            boolean forceWrapper,
            @NotNull List<Arg> args,
            @Nullable TypeConverter ret
    ) {

        public void appendCall(@NotNull MethodSpec.Builder method, @NotNull TypeName wrappedClass, @NotNull TypeName stateType, @NotNull String returnWord) {
            method.addStatement("final $T ref = ($T) state.checkUserDataArg(1, TYPE_NAME)", stateType, stateType);
            if (isDirect()) {
                method.addStatement("$L ref.$N(state)", returnWord, methodName());
                return;
            }

            for (int i = 0; i < args().size(); i++) {
                var arg = args().get(i);
                // Offset by 2 because we are 1 indexed and the first arg is the userdata object.
                arg.type().insertPop(method, arg.name(), i + 2);
            }
            String argNames = args().stream()
                    .map(MethodList.Arg::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            if (ret() != null) {
                method.addCode("var result = ");
            }

            if (isStatic) {
                method.addStatement("$T.$N(ref$L)", wrappedClass, methodName(), args.isEmpty() ? "" : ", " + argNames);
            } else {
                method.addStatement("ref.$N($L)", methodName(), argNames);
            }

            if (ret() != null) {
                ret().insertPush(method, "result");
            }

            method.addStatement("$L $L", returnWord, ret() == null ? 0 : 1);
        }
    }

    public static @NotNull MethodList collect(
            @NotNull Messager log,
            @NotNull Map<TypeName, TypeConverter> typeConverters,
            @NotNull TypeName userType,
            @NotNull List<? extends Element> elems
    ) {
        var methods = new ArrayList<Method>();
        for (var elem : elems) {
            if (!(elem instanceof ExecutableElement method)) continue;

            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
            var methodName = method.getSimpleName().toString();
            var name = Names.toMethodName(methodName);

            AnnotationMirror metaAnnotation = ProcUtil.getAnnotation(method, Types.LUA_META);
            AnnotationMirror methodAnnotation = ProcUtil.getAnnotation(method, Types.LUA_METHOD);
            if (metaAnnotation != null && methodAnnotation != null) {
                log.printError("Method cannot be both @LuaMeta and @LuaMethod", method);
                continue;
            }

            // The lua name of meta methods needs to be set to the meta method name.
            if (metaAnnotation != null) {
                VariableElement enumCase = ProcUtil.getAnnotationValue(metaAnnotation, "value", VariableElement.class);
                name = "__" + enumCase.getSimpleName().toString().toLowerCase(Locale.ROOT);
            }

            // A 'direct' method is one where the function drops down to basic lua impl, aka takes a LuaState
            // and returns the number of return values left on the stack. This is a special case for that type
            // of method.
            if (method.getParameters().size() == 1) {
                var arg = TypeName.get(method.getParameters().get(0).asType());
                var ret = TypeName.get(method.getReturnType());

                if (arg.equals(Types.LUA_STATE) && ret == TypeName.INT) {
                    methods.add(new Method(name, methodName, true, isStatic, false, List.of(), null));
                    continue;
                }
            }

            // If this is a meta function we need to get the method it is supposed to implement and ensure that
            // it matches the expected signature.
            if (metaAnnotation != null) {
                VariableElement enumCase = ProcUtil.getAnnotationValue(metaAnnotation, "value", VariableElement.class);
                MetaMethodType metaType = MetaMethodType.valueOf(enumCase.getSimpleName().toString());

                if (metaType.requiresDirect()) {
                    log.printError(enumCase + " requires a directly implemented metamethod", method);
                    continue;
                }
                if (!metaType.validate(log, userType, method)) continue;
            }

            var args = new ArrayList<Arg>();
            var params = method.getParameters();
            for (int i = isStatic ? 1 : 0; i < params.size(); i++) {
                var param = params.get(i);
                var type = typeConverters.get(TypeName.get(param.asType()));
                if (type == null) {
                    log.printError("Unsupported type: " + param.asType(), param);
                    continue;
                }
                args.add(new Arg(param.getSimpleName().toString(), type));
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

            methods.add(new Method(name, methodName, false, isStatic, false, args, ret));
        }
        return new MethodList(methods);
    }

    public boolean isEmpty() {
        return methods.isEmpty();
    }

}
