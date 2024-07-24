package net.hollowcube.luau.ap;

import com.squareup.javapoet.TypeName;
import net.hollowcube.luau.ap.tree.Node;
import net.hollowcube.luau.ap.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public final class Methods {

    public static @Nullable Node.Method.Actual single(
            @NotNull Messager log,
            @NotNull Elements elements,
            @NotNull LuaTypeRegistry types,
            @NotNull ExecutableElement method,
            @NotNull String name, @NotNull String methodName
    ) {
        boolean isStatic = method.getModifiers().contains(Modifier.STATIC);

        var args = new ArrayList<Node.Method.Arg>();
        var params = method.getParameters();
        for (int i = isStatic ? 1 : 0; i < params.size(); i++) {
            var param = params.get(i);

            var paramType = types.forTypeMirror(param.asType());
            if (paramType == null) {
                log.printError("Unsupported type: " + param.asType(), param);
                continue;
            }

            args.add(new Node.Method.Arg(param.getSimpleName().toString(), paramType));
        }
        // Defer return from args here to show multiple errors if there are them
        if (args.size() != params.size() - (isStatic ? 1 : 0))
            return null;

        LuaTypeMirror ret = null;
        TypeName retType = TypeName.get(method.getReturnType());
        if (retType != TypeName.VOID) {
            ret = types.forTypeMirror(method.getReturnType());
            if (ret == null) {
                log.printError("Unsupported return type: " + retType, method);
                return null;
            }
        }

        var doc = DocContent.parse(elements.getDocComment(method));
        return new Node.Method.Actual(method, name, methodName, false, isStatic, false, args, ret, doc);
    }

    public static @NotNull List<Node.Method> collect(
            @NotNull Messager log,
            @NotNull Elements elements,
            @NotNull LuaTypeRegistry types,
            @NotNull TypeName userType,
            @NotNull List<? extends Element> elems
    ) {
        var lastMethods = new HashMap<String, Integer>(); // name to index in methods list
        var methods = new ArrayList<Node.Method>();
        for (var elem : elems) {
            if (!(elem instanceof ExecutableElement method)) continue;

            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
            var methodName = method.getSimpleName().toString();
            var name = toMethodName(methodName);

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
                    var doc = DocContent.parse(elements.getDocComment(method));
                    methods.add(new Node.Method.Actual(method, name, methodName, true, isStatic, false, List.of(), null, doc));
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
                if (!metaType.validate(log, userType, method))
                    continue;
            }

            var meth = single(log, elements, types, method, name, methodName);
            if (meth == null) continue;

            // Create an overload if this is one
            var lastIndex = lastMethods.get(name);
            if (lastIndex == null) {
                lastMethods.put(name, methods.size());
                methods.add(meth);
            } else {
                var lastMethod = methods.get(lastIndex);
                if (lastMethod instanceof Node.Method.Overload overload) {
                    overload.overloads().add(meth);
                } else {
                    var overload = new Node.Method.Overload(method, name, new ArrayList<>());
                    overload.overloads().add((Node.Method.Actual) lastMethod);
                    overload.overloads().add(meth);
                    methods.set(lastIndex, overload);
                }
            }
        }

        return methods;
    }

    private static @NotNull String toMethodName(@NotNull String methodName) {
        return Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
    }

}
