package net.hollowcube.luau.ap.proc;

import com.squareup.javapoet.*;
import net.hollowcube.luau.ap.MethodList;
import net.hollowcube.luau.ap.PropertyList;
import net.hollowcube.luau.ap.TypeConverter;
import net.hollowcube.luau.ap.Types;
import net.hollowcube.luau.ap.util.ProcUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Map;

public class LuaObjectProcessor extends AbstractLuaProcessor {

    public LuaObjectProcessor(@NotNull Messager log, @NotNull Elements elements, @NotNull Map<TypeName, TypeConverter> typeConverters) {
        super(log, elements, typeConverters);
    }

    @Override
    public @Nullable TypeSpec process(@NotNull TypeElement typeElem) {
        var packageName = elements.getPackageOf(typeElem).getQualifiedName().toString();
        var wrappedClass = TypeName.get(typeElem.asType());
        if (wrappedClass instanceof ParameterizedTypeName ptn) {
            wrappedClass = ptn.rawType;
        }
        var className = typeElem.getSimpleName().toString() + "$Wrapper";
        var wrapperClass = ClassName.get(packageName, className);

        // Find the @LuaProperty, @LuaMethod declarations
        var properties = PropertyList.collect(log, typeConverters,
                ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_PROPERTY));
        var methods = MethodList.collect(log, typeConverters, wrappedClass,
                ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_METHOD));
        var metaMethods = MethodList.collect(log, typeConverters, wrappedClass,
                ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_META));

        TypeSpec.Builder wrapper = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Gen a constant for the metatable name
        wrapper.addField(FieldSpec.builder(String.class, "TYPE_NAME")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.class.getName()", wrappedClass)
                .build());

        // In these types we just dont support __index or __namecall as user defined metamethods
        // because we need to override them for property & method support.
        for (var metaMethod : metaMethods.methods()) {
            if ((!properties.isEmpty() && metaMethod.name().equals("__index"))
                    || (!methods.isEmpty() && metaMethod.name().equals("__namecall"))) {
                log.printError("Cannot define metamethod " + metaMethod.name() + " in a LuaObject", typeElem);
                return null;
            }
        }

        // Prepend our own two metamethods for __index and __namecall
        var metaMethodList = new ArrayList<>(metaMethods.methods());
        if (!properties.isEmpty())
            metaMethodList.addFirst(new MethodList.Method("__index", "generatedLuaIndex", true, true, true, new ArrayList<>(), null));
        if (!methods.isEmpty())
            metaMethodList.addFirst(new MethodList.Method("__namecall", "generatedLuaNameCall", true, true, true, new ArrayList<>(), null));

        appendInitFunc(wrapper, wrappedClass, wrapperClass, wrappedClass, null, new MethodList(metaMethodList));
        if (!properties.isEmpty()) wrapper.addMethod(buildIndexMetaMethod(wrappedClass, properties));
        if (!methods.isEmpty()) wrapper.addMethod(buildNameCallMetaMethod(wrappedClass, methods));

        return wrapper.build();
    }

    private @NotNull MethodSpec buildIndexMetaMethod(@NotNull TypeName wrappedClass, @NotNull PropertyList properties) {
        var method = MethodSpec.methodBuilder("generatedLuaIndex")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        method.returns(int.class);
        method.addParameter(Types.LUA_STATE, "state");
        method.addStatement("final $T ref = ($T) state.checkUserDataArg(1, TYPE_NAME)", wrappedClass, wrappedClass);
        method.addStatement("final String key = state.checkStringArg(2)");

        method.addCode("return switch (key) {$>\n");

        for (var prop : properties.properties()) {
            method.addCode("case $S -> {$>\n", prop.name());

            if (prop.isPin()) {
                method.addStatement("(($T) ref.$L).push(state)", Types.PIN_IMPL, prop.accessor());
                method.addStatement("yield 1");
            } else {
                prop.type().insertPush(method, "ref." + prop.accessor());
                method.addStatement("yield 1");
            }

            method.addCode("$<}\n");
        }

        method.addCode("default -> {$>\n");
        method.addStatement("state.argError(2, \"No such key: \" + key)");
        method.addStatement("yield 0");
        method.addCode("$<}\n");

        method.addCode("$<};");

        return method.build();
    }

    public @NotNull MethodSpec buildNameCallMetaMethod(@NotNull TypeName wrappedClass, @NotNull MethodList methods) {
        var method = MethodSpec.methodBuilder("generatedLuaNameCall")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        method.returns(int.class);
        method.addParameter(Types.LUA_STATE, "state");
        method.addStatement("final $T ref = ($T) state.checkUserDataArg(1, TYPE_NAME)", wrappedClass, wrappedClass);
        method.addStatement("String methodName = state.nameCallAtom()");

        method.addCode("return switch (methodName) {$>\n");
        for (var meth : methods.methods()) {
            method.addCode("case $S -> {$>\n", meth.name());

            if (meth.isDirect()) {
                method.addStatement("yield ref.$N(state)", meth.methodName());
            } else {
                for (int i = 0; i < meth.args().size(); i++) {
                    var arg = meth.args().get(i);
                    // Offset by 2 because we are 1 indexed and the first arg is the userdata object.
                    arg.type().insertPop(method, arg.name(), i + 2);
                }

                if (meth.ret() != null) {
                    method.addCode("var result = ");
                }
                method.addStatement("ref.$N($L)", meth.methodName(), meth.args().stream()
                        .map(MethodList.Arg::name)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse(""));

                if (meth.ret() != null) {
                    meth.ret().insertPush(method, "result");
                }

                method.addStatement("yield $L", meth.ret() == null ? 0 : 1);
            }

            method.addCode("$<}\n");
        }

        method.addCode("default -> {$>\n");
        method.addStatement("state.error(\"No such method: \" + methodName)");
        method.addStatement("yield 0");
        method.addCode("$<}\n");

        method.addCode("$<};");

        return method.build();
    }

}
