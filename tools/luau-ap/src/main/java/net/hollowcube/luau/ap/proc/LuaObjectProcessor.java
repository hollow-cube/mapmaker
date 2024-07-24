package net.hollowcube.luau.ap.proc;

import com.google.gson.GsonBuilder;
import com.squareup.javapoet.*;
import net.hollowcube.luau.ap.Methods;
import net.hollowcube.luau.ap.Properties;
import net.hollowcube.luau.ap.Types;
import net.hollowcube.luau.ap.tree.LuaDocBuilder;
import net.hollowcube.luau.ap.tree.LuaNameCallBuilder;
import net.hollowcube.luau.ap.tree.LuaTypeDefinitionBuilder;
import net.hollowcube.luau.ap.tree.Node;
import net.hollowcube.luau.ap.util.DocContent;
import net.hollowcube.luau.ap.util.LuaTypeRegistry;
import net.hollowcube.luau.ap.util.ProcUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import java.util.ArrayList;

public class LuaObjectProcessor extends AbstractLuaProcessor {

    public LuaObjectProcessor(@NotNull Messager log, @NotNull Elements elements, @NotNull LuaTypeRegistry types) {
        super(log, elements, types);
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

        ClassName superClassType = null;
        if (!typeElem.getModifiers().contains(Modifier.FINAL)) {
            var superClass = typeElem.getSuperclass();
            if (superClass.getKind() == TypeKind.NONE || "java.lang.Object".equals(superClass.toString())) {
                // These are both nothing
                superClass = null;
            }

            superClassType = superClass != null ? (ClassName) Types.unwrap(TypeName.get(superClass)) : null;
        }

        // Find the @LuaProperty, @LuaMethod declarations
        var properties = Properties.collect(log, elements, types,
                ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_PROPERTY));
        var methods = Methods.collect(log, elements, types, wrappedClass,
                ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_METHOD));
        var metaMethods = Methods.collect(log, elements, types, wrappedClass,
                ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_META));
        var typeDoc = DocContent.parse(elements.getDocComment(typeElem));

        var typeNode = new Node.Type(
                typeElem.getSimpleName().toString().replace("Lua", ""),
                typeElem, wrappedClass, wrapperClass, superClassType,
                typeDoc, properties, methods
        );

        TypeSpec.Builder wrapper = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Gen a constant for the metatable name
        wrapper.addField(FieldSpec.builder(String.class, "TYPE_NAME")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.class.getName()", wrappedClass)
                .build());

        // In these types we just dont support __index or __namecall as user defined metamethods
        // because we need to override them for property & method support.
        for (var metaMethod : metaMethods) {
            if ((!properties.isEmpty() && metaMethod.name().equals("__index"))
                    || (!methods.isEmpty() && metaMethod.name().equals("__namecall"))) {
                log.printError("Cannot define metamethod " + metaMethod.name() + " in a LuaObject", typeElem);
                return null;
            }
        }

        // Prepend our own two metamethods for __index and __namecall
        var metaMethodList = new ArrayList<>(metaMethods);
        if (!properties.isEmpty())
            metaMethodList.addFirst(new Node.Method.Actual(typeElem, "__index", "generatedLuaIndex", true, true, true, new ArrayList<>(), null, null));
        if (!methods.isEmpty())
            metaMethodList.addFirst(new Node.Method.Actual(typeElem, "__namecall", "generatedLuaNameCall", true, true, true, new ArrayList<>(), null, null));
        metaMethods = metaMethodList;

        appendInitFunc(wrapper, wrappedClass, wrapperClass, wrappedClass, null, metaMethods);
        appendIndexMetaMethod(wrapper, typeNode);
        appendNameCallMetaMethod(wrapper, typeNode);
        //todo meta methods
        appendTypeDocMethods(wrapper, typeNode);

        return wrapper.build();
    }

    private void appendIndexMetaMethod(@NotNull TypeSpec.Builder wrapper, @NotNull Node.Type typeNode) {
        if (typeNode.properties().isEmpty()) return;

        var method = MethodSpec.methodBuilder("generatedLuaIndex")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC);

        method.returns(int.class);
        method.addParameter(Types.LUA_STATE, "state");
        method.addStatement("final $T ref = $T.checkUserDataArg(state, 1, $T.class)",
                typeNode.implClass(), Types.LUA_HELPERS, typeNode.implClass());
        method.addStatement("final String key = state.checkStringArg(2)");

        method.addCode("return switch (key) {$>\n");

        for (var prop : typeNode.properties()) {
            method.addCode("case $S -> {$>\n", prop.name());

            prop.type().insertPush(method, "ref." + prop.accessor());
            method.addStatement("yield 1");

            method.addCode("$<}\n");
        }

        method.addCode("default -> {$>\n");
        method.addStatement("state.argError(2, \"No such key: \" + key)");
        method.addStatement("yield 0");
        method.addCode("$<}\n");

        method.addCode("$<};");
        wrapper.addMethod(method.build());
    }

    public void appendNameCallMetaMethod(@NotNull TypeSpec.Builder wrapper, @NotNull Node.Type typeNode) {
        if (typeNode.methods().isEmpty()) return;
        wrapper.addMethod(LuaNameCallBuilder.create(typeNode));
    }

    private void appendTypeDocMethods(
            @NotNull TypeSpec.Builder type,
            @NotNull Node.Type typeNode
    ) {
        var generatedLuaTypesMethod = MethodSpec.methodBuilder("generatedLuaTypes")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class);
        var typeString = LuaTypeDefinitionBuilder.buildTypeDefObject(typeNode);
        generatedLuaTypesMethod.addStatement("return $S", typeString);
        type.addMethod(generatedLuaTypesMethod.build());

        var generatedLuaDocMethod = MethodSpec.methodBuilder("generatedLuaDocs")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(String.class);
        var docString = LuaDocBuilder.buildDocObject(typeNode);
        generatedLuaDocMethod.addStatement("return $S", new GsonBuilder().setPrettyPrinting().create().toJson(docString));
        type.addMethod(generatedLuaDocMethod.build());
    }

}
