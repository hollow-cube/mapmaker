package net.hollowcube.luau.slopgen;

import com.palantir.javapoet.*;
import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.luau.gen.LuaProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner14;
import java.lang.foreign.Arena;

public class LuaLibraryClassBuildingVisitor extends ElementScanner14<Void, Void> {

    private static final ParameterSpec NOT_NULL_STATE = ParameterSpec.builder(LuaState.class, "state")
        .addAnnotation(NotNull.class)
        .build();

    private final Messager messager;
    private final StringAtomizer atomizer;

    private final TypeName glueTypeName;
    private final TypeSpec.Builder glueTypeBuilder;

    private final MethodSpec.Builder registerMethod;

    // Reassigned as we traverse.
    private TypeName typeName;
    private @Nullable String luaTypeName = null;
    private MethodSpec.Builder indexMethod;
    private MethodSpec.Builder namecallMethod;

    public LuaLibraryClassBuildingVisitor(
        ProcessingEnvironment env,
        StringAtomizer atomizer,
        TypeName typeName,
        TypeName glueTypeName,
        TypeSpec.Builder glueTypeBuilder
    ) {
        this.messager = env.getMessager();
        this.atomizer = atomizer;
        this.typeName = typeName;
        this.glueTypeName = glueTypeName;
        this.glueTypeBuilder = glueTypeBuilder;

        this.registerMethod = beginRegisterMethod();
        this.indexMethod = beginIndexMethod();
        this.namecallMethod = beginNamecallMethod();
    }

    public void finish() {
        glueTypeBuilder.addMethod(endRegisterMethod());
        glueTypeBuilder.addMethod(endIndexMethod());
        glueTypeBuilder.addMethod(endNamecallMethod());
    }

    @Override
    public Void visitType(TypeElement e, Void unused) {
        // The top level class can be ignored
        if (e.getEnclosingElement().getKind() == ElementKind.PACKAGE)
            return super.visitType(e, unused);

        if (e.getAnnotation(LuaExport.class) == null)
            return null; // Not exported, continue

        // We need to enter the inner type, process, then return

        TypeName lastTypeName = typeName;
        typeName = TypeName.get(e.asType());
        String lastLuaTypeName = luaTypeName;
        luaTypeName = e.getSimpleName().toString();
        MethodSpec.Builder lastIndexMethod = indexMethod;
        indexMethod = beginIndexMethod();
        MethodSpec.Builder lastNamecallMethod = namecallMethod;
        namecallMethod = beginNamecallMethod();

        setupNamedType();

        super.visitType(e, unused);

        glueTypeBuilder.addMethod(endNamecallMethod());
        namecallMethod = lastNamecallMethod;
        glueTypeBuilder.addMethod(endIndexMethod());
        indexMethod = lastIndexMethod;
        luaTypeName = lastLuaTypeName;
        typeName = lastTypeName;

        return null;
    }

    @Override
    public Void visitExecutable(ExecutableElement e, Void unused) {
        boolean isProperty = e.getAnnotation(LuaProperty.class) != null;
        boolean isMethod = e.getAnnotation(LuaMethod.class) != null;
        boolean isStatic = e.getModifiers().contains(Modifier.STATIC);

        if (!isProperty && !isMethod) return null;
        if (!isStatic && luaTypeName == null) {
            messager.printError("Only static methods can be exported from library classes", e);
            return null;
        }
        if (isStatic && luaTypeName != null) {
            messager.printError("Only non-static methods can be exported from exported type classes", e);
            return null;
        }

        if (isProperty) appendIndexCall(e, isStatic);
        else if (isMethod) appendNamecallCall(e);
        else throw new IllegalStateException("not namecall or method");

        return null;
    }

    private void appendIndexCall(ExecutableElement e, boolean isStatic) {
        var javaName = e.getSimpleName().toString();
        var luaName = LuaNames.toLuaProperty(javaName);

        indexMethod.addCode("case $L/*$L*/ -> ", atomizer.atomize(luaName), luaName);
        if (isStatic) {
            indexMethod.addStatement("$T.$L(state)",
                TypeName.get(e.getEnclosingElement().asType()), javaName);
        } else {
            indexMethod.addStatement("self.$L(state)", javaName);
        }
    }

    private void appendNamecallCall(ExecutableElement e) {
        var javaName = e.getSimpleName().toString();
        var luaName = LuaNames.toLuaMethod(javaName);

        if (luaTypeName == null) {
            // Ends up actually being an index to the direct function reference.
            var luaFuncField = namespace(luaName, true);
            glueTypeBuilder.addField(FieldSpec.builder(LuaFunc.class, luaFuncField)
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.wrap($T::$L, $S)", LuaFunc.class, e.getEnclosingElement(), javaName, luaName)
                .build());
            indexMethod.beginControlFlow("case $L/*$L*/ -> ", atomizer.atomize(luaName), luaName);
            indexMethod.addStatement("state.pushFunction($L)", luaFuncField);
            indexMethod.addStatement("yield 1");
            indexMethod.endControlFlow();
        } else {
            namecallMethod.beginControlFlow("case $L/*$L*/ -> ", atomizer.atomize(luaName), luaName);
            // When calling a non-static method, we remove the self argument
            // so that the callee can pretend its a 'normal' call.
            namecallMethod.addStatement("state.remove(1)");
            namecallMethod.addStatement("yield self.$L(state)", javaName);
            namecallMethod.endControlFlow();
        }
    }

    private MethodSpec.Builder beginRegisterMethod() {
        var method = MethodSpec.methodBuilder("register")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(LuaState.class, "state");

        {   // Module value setup
            // For the library object we dont actually care about the
            // userdata itself, none of the 'static' functions use it.
            method.addStatement("state.newUserData(new Object())");
            method.addStatement("state.newTable()"); // metatable

            method.addStatement("state.pushFunction(INDEX)");
            method.addStatement("state.setField(-2, $S)", LuaNames.INDEX_META_NAME);
            method.addStatement("state.pushFunction(NAMECALL)");
            method.addStatement("state.setField(-2, $S)", LuaNames.NAMECALL_META_NAME);

            method.addStatement("state.setReadOnly(-1, true)"); // metatable read only
            method.addStatement("state.setMetaTable(-2)"); // assign metatable to userdata

            method.addStatement("state.requireRegisterModule(LIB_NAME)"); // register for require
            // userdata is popped during register.
        }

        return method;
    }

    private MethodSpec endRegisterMethod() {

        return registerMethod.build();
    }

    private MethodSpec.Builder beginIndexMethod() {
        var glueName = namespace(LuaNames.INDEX_GLUE_NAME, false);
        createLuaFunc(namespace("INDEX", true), glueName, LuaNames.INDEX_META_NAME);

        var method = MethodSpec.methodBuilder(glueName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);

        // If member, need to add read for self parameter
        if (luaTypeName != null) {
            method.addStatement("$T self = check$LArg(state, 1)", typeName, luaTypeName);
        }

        method.addStatement("short indexAtom = state.toStringAtomRaw(2)");
        method.beginControlFlow("return switch (indexAtom)");

        return method;
    }

    private MethodSpec endIndexMethod() {
        // There are three remaining scenarios here:
        // 1. the atom is NO_ATOM in which case we dont need to do anything ever
        indexMethod.addStatement("case $T.NO_ATOM -> 0", LuaState.class);

        // 2. if we have a superclass, delegate to that in default
        // TODO: handle this case
        // indexMethod.addStatement("default -> $T.super.index$meta(state)", glueSuperType);

        // 3. no superclass, just exit
        indexMethod.addStatement("default -> 0");
        indexMethod.addCode("$<};");

        return indexMethod.build();
    }

    private MethodSpec.Builder beginNamecallMethod() {
        var glueName = namespace(LuaNames.NAMECALL_GLUE_NAME, false);
        createLuaFunc(namespace("NAMECALL", true), glueName, LuaNames.NAMECALL_META_NAME);

        var method = MethodSpec.methodBuilder(glueName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);

        // If member, need to add read for self parameter
        if (luaTypeName != null) {
            method.addStatement("$T self = check$LArg(state, 1)", typeName, luaTypeName);
        }

        method.addStatement("short namecallAtom = state.nameCallAtomRaw()");
        method.beginControlFlow("return switch (namecallAtom)");

        return method;
    }

    private MethodSpec endNamecallMethod() {
        // TODO: should be an error here i guess
        namecallMethod.addStatement("default -> 0");
        namecallMethod.addCode("$<};");

        return namecallMethod.build();
    }

    private void createLuaFunc(String name, String methodName, String debugName) {
        glueTypeBuilder.addField(FieldSpec.builder(LuaFunc.class, name)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$T.wrap($T::$L, $S, $T.global())",
                LuaFunc.class, glueTypeName, methodName, debugName, Arena.class)
            .build());
    }

    private void setupNamedType() {
        var tagName = namespace("TAG", true);

        glueTypeBuilder.addField(FieldSpec.builder(int.class, tagName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer(String.valueOf(atomizer.userDataTag()))
            .build());

        // Push to stack method
        glueTypeBuilder.addMethod(MethodSpec.methodBuilder("push" + luaTypeName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(NOT_NULL_STATE)
            .addParameter(typeName, "obj")
            .addStatement("state.newUserDataTaggedWithMetatable(obj, $L)", tagName)
            .build());

        // Check stack arg method
        glueTypeBuilder.addMethod(MethodSpec.methodBuilder("check" + luaTypeName + "Arg")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(NOT_NULL_STATE)
            .addParameter(int.class, "argIndex")
            .returns(typeName.annotated(AnnotationSpec.builder(NotNull.class).build()))
            .addStatement("Object obj = state.toUserDataTagged(argIndex, $L)", tagName)
            .beginControlFlow("if (obj instanceof $T actual)", typeName)
            .addStatement("return actual")
            .endControlFlow()
            .addStatement("state.typeError(argIndex, $S)", luaTypeName)
            .addStatement("return null")
            .build());

        // Append setup logic to register
        registerMethod.addCode("\n");
        registerMethod.addComment("Type setup for $L", luaTypeName);

        registerMethod.addStatement("state.newTable()"); // metatable

        registerMethod.addStatement("state.pushFunction($L)", namespace("INDEX", true));
        registerMethod.addStatement("state.setField(-2, $S)", LuaNames.INDEX_META_NAME);
        registerMethod.addStatement("state.pushFunction($L)", namespace("NAMECALL", true));
        registerMethod.addStatement("state.setField(-2, $S)", LuaNames.NAMECALL_META_NAME);

        registerMethod.addStatement("state.setReadOnly(-1, true)"); // metatable read only
        registerMethod.addStatement("state.setUserDataMetaTable($L)", tagName); // assign metatable to userdata
    }

    private String namespace(String key, boolean upper) {
        if (luaTypeName == null) return upper ? key.toUpperCase() : key; // nothing needed
        return upper
            ? luaTypeName.toUpperCase() + "_" + key.toUpperCase()
            : luaTypeName.toLowerCase() + "$" + key;
    }

}
