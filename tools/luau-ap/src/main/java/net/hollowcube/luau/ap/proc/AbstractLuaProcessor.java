package net.hollowcube.luau.ap.proc;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.hollowcube.luau.ap.MethodList;
import net.hollowcube.luau.ap.TypeConverter;
import net.hollowcube.luau.ap.Types;
import net.hollowcube.luau.ap.util.LuaTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractLuaProcessor {

    protected final Messager log;
    protected final Elements elements;

    protected final Map<TypeName, TypeConverter> typeConverters;
    protected final LuaTypeRegistry types;

    protected AbstractLuaProcessor(@NotNull Messager log, @NotNull Elements elements, @NotNull Map<TypeName, TypeConverter> typeConverters, @NotNull LuaTypeRegistry types) {
        this.log = log;
        this.elements = elements;
        this.typeConverters = typeConverters;
        this.types = types;
    }

    public abstract @Nullable TypeSpec process(@NotNull TypeElement typeElem);

    protected void appendInitFunc(
            @NotNull TypeSpec.Builder type,
            @NotNull TypeName wrappedClass,
            @NotNull TypeName wrapperClass,
            @NotNull TypeName targetType,
            @Nullable TypeName targetImplClass,
            @NotNull MethodList metaMethods
    ) {
        var method = MethodSpec.methodBuilder("initMetatable")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        method.addParameter(Types.LUA_STATE, "state");
        method.addStatement("state.newMetaTable(TYPE_NAME)");

        List<MethodSpec> proxyMethods = new ArrayList<>();
        for (var metaMethod : metaMethods.methods()) {
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
            metaMethod.appendCall(proxy, wrappedClass, targetType, targetImplClass, "return");
            proxyMethods.add(proxy.build());
        }

        method.addStatement("state.pop(1)");
        type.addMethod(method.build());
        proxyMethods.forEach(type::addMethod);
    }

}
