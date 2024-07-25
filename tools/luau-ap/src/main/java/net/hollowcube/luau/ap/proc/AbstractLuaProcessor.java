package net.hollowcube.luau.ap.proc;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.hollowcube.luau.ap.Types;
import net.hollowcube.luau.ap.tree.Node;
import net.hollowcube.luau.ap.util.LuaTypeMirror;
import net.hollowcube.luau.ap.util.LuaTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractLuaProcessor {

    protected final Messager log;
    protected final Elements elements;

    protected final LuaTypeRegistry types;

    protected AbstractLuaProcessor(@NotNull Messager log, @NotNull Elements elements, @NotNull LuaTypeRegistry types) {
        this.log = log;
        this.elements = elements;
        this.types = types;
    }

    public abstract @Nullable TypeSpec process(@NotNull TypeElement typeElem);

    protected void appendInitFunc(
            @NotNull TypeSpec.Builder type,
            Node.@UnknownNullability Type typeNode,
            @NotNull TypeName wrappedClass,
            @NotNull TypeName wrapperClass,
            @NotNull TypeName targetType,
            @Nullable TypeName targetImplClass,
            @NotNull List<Node.Method> metaMethods,
            @NotNull List<Node.Method.Actual> staticMethods,
            boolean isSingleton
    ) {
        var method = MethodSpec.methodBuilder("initMetatable")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addParameter(Types.LUA_STATE, "state");
        List<MethodSpec> proxyMethods = new ArrayList<>();

        // Create the metatable for the type value
        if (!isSingleton) {
            method.addComment("Type metatable definition");
            method.addStatement("state.newMetaTable(TYPE_NAME)");

            for (var metaMeth : metaMethods) {
                if (!(metaMeth instanceof Node.Method.Actual metaMethod)) {
                    log.printError("Overloads are not allowed on meta methods", metaMeth.elem());
                    continue;
                }
                // For static, direct metamethods we can push it as a c function directly.
                if (metaMethod.isDirect() && metaMethod.isStatic()) {
                    final TypeName target = metaMethod.forceWrapper() ? wrapperClass : wrappedClass;
                    method.addStatement("state.pushCFunction($T::$L, $S)", target, metaMethod.methodName(), metaMethod.name());
                    method.addStatement("state.setField(-2, $S)", metaMethod.name());
                    continue;
                }

                // Otherwise we need to generate a proxy method also.
                method.addStatement("state.pushCFunction($T::$L, $S)", wrapperClass, metaMethod.methodName(), metaMethod.name());
                method.addStatement("state.setField(-2, $S)", metaMethod.name());

                MethodSpec.Builder proxy = MethodSpec.methodBuilder(metaMethod.methodName())
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addParameter(Types.LUA_STATE, "state")
                        .returns(TypeName.INT);
                appendCall(metaMethod, proxy, wrappedClass, targetType, targetImplClass, "return");
                proxyMethods.add(proxy.build());
            }

            method.addStatement("state.pop(1)");
        }

        // Create the table for static methods if present
        if (!staticMethods.isEmpty()) {
            method.addCode("\n");
            method.addComment("Static methods object");
            method.addStatement("state.newTable()");

            for (var staticMeth : staticMethods) {
                // For direct metamethods we can push it as a c function directly.
                if (staticMeth.isDirect()) {
                    final TypeName target = staticMeth.forceWrapper() ? wrapperClass : wrappedClass;
                    method.addStatement("state.pushCFunction($T::$L, $S)", target, staticMeth.methodName(), staticMeth.name());
                    method.addStatement("state.setField(-2, $S)", staticMeth.name());
                    continue;
                }

                // Otherwise we need to generate a proxy method also.
                method.addStatement("state.pushCFunction($T::$L, $S)", wrapperClass, staticMeth.methodName(), staticMeth.name());
                method.addStatement("state.setField(-2, $S)", staticMeth.name());

                MethodSpec.Builder proxy = MethodSpec.methodBuilder(staticMeth.methodName())
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addParameter(Types.LUA_STATE, "state")
                        .returns(TypeName.INT);
                appendCall(staticMeth, proxy, wrappedClass, targetType, targetImplClass, "return");
                proxyMethods.add(proxy.build());
            }

            method.addStatement("state.setReadOnly(-1, true)");
            method.addStatement("state.setGlobal($S)", typeNode.name());
        }

        type.addMethod(method.build());
        proxyMethods.forEach(type::addMethod);
    }

    private void appendCall(@NotNull Node.Method.Actual meth, @NotNull MethodSpec.Builder method, @NotNull TypeName wrappedClass, @NotNull TypeName stateType, @Nullable TypeName typeImplClass, @NotNull String returnWord) {
        if (!meth.isStatic()) {
            if (typeImplClass != null) {
                method.addStatement("final $T ref = $T.checkLuaArg(state, 1)", stateType, typeImplClass);
            } else {
                method.addStatement("final $T ref = ($T) $T.checkUserDataArg(state, 1, $T.class)", stateType, stateType, Types.LUA_HELPERS, wrappedClass);
            }
        }
        if (meth.isDirect()) {
            method.addStatement("$L ref.$N(state)", returnWord, meth.methodName());
            return;
        }

        int argIndex = 1; // 1 indexed language
        var argNames = new StringBuilder();
        if (!meth.args().isEmpty() && meth.args().get(0).type() != LuaTypeMirror.LUA_STATE_MARKER) {
            argIndex++;
        }
        for (var arg : meth.args()) {
            if (!argNames.isEmpty()) argNames.append(", ");

            if (arg.type() == LuaTypeMirror.LUA_STATE_MARKER) {
                argNames.append("state");
                continue; // Do not increment index, this is a synthetic arg
            } else if (arg.type().javaType().equals(stateType)) {
                if (typeImplClass != null) {
                    method.addStatement("final $T ref = $T.checkLuaArg(state, 1)", stateType, typeImplClass);
                } else {
                    method.addStatement("final $T ref = ($T) $T.checkUserDataArg(state, 1, $T.class)", stateType, stateType, Types.LUA_HELPERS, wrappedClass);
                }
                argNames.append("ref");
                argIndex++;
                continue;
            }

            arg.type().insertPop(method, arg.name(), argIndex++);

            argNames.append(arg.name());
        }

        if (meth.ret() != null) {
            method.addCode("var result = ");
        }

        if (meth.isStatic()) {
            method.addStatement("$T.$N($L)", wrappedClass, meth.methodName(), meth.args().isEmpty() ? "" : argNames);
        } else {
            method.addStatement("ref.$N($L)", meth.methodName(), argNames);
        }

        if (meth.ret() != null) {
            meth.ret().insertPush(method, "result");
        }

        method.addStatement("$L $L", returnWord, meth.ret() == null ? 0 : 1);
    }

}
