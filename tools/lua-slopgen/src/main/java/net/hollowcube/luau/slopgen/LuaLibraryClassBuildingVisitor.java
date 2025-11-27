package net.hollowcube.luau.slopgen;

import com.palantir.javapoet.*;
import net.hollowcube.luau.LuaFunc;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.luau.gen.Meta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementScanner14;
import java.lang.foreign.Arena;
import java.util.*;

public class LuaLibraryClassBuildingVisitor extends ElementScanner14<Void, Void> {

    private static final ParameterSpec NOT_NULL_STATE = ParameterSpec.builder(LuaState.class, "state")
        .addAnnotation(NotNull.class)
        .build();

    private final Messager messager;
    private final StringAtomizer atomizer;

    private final TypeName glueTypeName;
    private final TypeSpec.Builder glueTypeBuilder;
    private final boolean isGlobal;

    private final MethodSpec.Builder registerMethod;

    private final Set<TypeName> dispatchTypes = new HashSet<>();
    private final Map<TypeName, List<Map.Entry<TypeName, String>>> dispatchIndexMethods = new HashMap<>();

    // Reassigned as we traverse.
    private TypeName typeName;
    private @Nullable String luaTypeName = null;
    private boolean isDispatch = false;
    private MethodSpec.Builder indexMethod;
    private MethodSpec.Builder newIndexMethod;
    private MethodSpec.Builder namecallMethod;

    public LuaLibraryClassBuildingVisitor(
        ProcessingEnvironment env,
        StringAtomizer atomizer,
        TypeName typeName,
        TypeName glueTypeName,
        TypeSpec.Builder glueTypeBuilder,
        boolean isGlobal
    ) {
        this.messager = env.getMessager();
        this.atomizer = atomizer;
        this.typeName = typeName;
        this.glueTypeName = glueTypeName;
        this.glueTypeBuilder = glueTypeBuilder;
        this.isGlobal = isGlobal;

        this.registerMethod = beginRegisterMethod();
        this.indexMethod = beginIndexMethod();
        this.newIndexMethod = beginNewIndexMethod();
        this.namecallMethod = beginNamecallMethod();
    }

    public void finish() {
        glueTypeBuilder.addMethod(endRegisterMethod());
        glueTypeBuilder.addMethod(endIndexMethod(null));
        glueTypeBuilder.addMethod(endNewIndexMethod(null));
        glueTypeBuilder.addMethod(endNamecallMethod(null));
    }

    @Override
    public Void visitType(TypeElement e, Void unused) {
        // The top level class can be ignored
        if (e.getEnclosingElement().getKind() == ElementKind.PACKAGE) {
            super.visitType(e, unused);

            // Finalize inherited types

            for (var type : dispatchTypes) {
                var oldLuaTypeName = luaTypeName;
                luaTypeName = ((ClassName) type).simpleName();

                MethodSpec.Builder index, newindex, namecall;
                {   // __index metamethod
                    var glueName = namespace("index$meta", false);
                    createLuaFunc(namespace("INDEX", true), glueName, LuaNames.INDEX_META_NAME);

                    var method = MethodSpec.methodBuilder(glueName)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addParameter(LuaState.class, "state")
                        .returns(int.class);

                    method.addStatement("$T self = check$LArg(state, 1)", type, luaTypeName);
                    method.addStatement("short atom = state.toStringAtomRaw(2)");

                    method.beginControlFlow("return switch (self)");
                    index = method;
                }
                {   // __newindex metamethod
                    var glueName = namespace("newindex$meta", false);
                    createLuaFunc(namespace("NEWINDEX", true), glueName, LuaNames.NEWINDEX_META_NAME);

                    var method = MethodSpec.methodBuilder(glueName)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addParameter(LuaState.class, "state")
                        .returns(int.class);

                    method.addStatement("$T self = check$LArg(state, 1)", type, luaTypeName);
                    method.addStatement("state.remove(1)");
                    method.addStatement("short atom = state.toStringAtomRaw(1)");
                    method.addStatement("state.remove(1)");

                    method.beginControlFlow("return switch (self)");
                    newindex = method;
                }
                {   // __namecall metamethod
                    var glueName = namespace("namecall$meta", false);
                    createLuaFunc(namespace("NAMECALL", true), glueName, LuaNames.NAMECALL_META_NAME);

                    var method = MethodSpec.methodBuilder(glueName)
                        .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                        .addParameter(LuaState.class, "state")
                        .returns(int.class);

                    method.addStatement("$T self = check$LArg(state, 1)", type, luaTypeName);
                    method.addStatement("short atom = state.nameCallAtomRaw()");

                    method.beginControlFlow("return switch (self)");
                    namecall = method;
                }

                var seenIndexMethods = new HashSet<String>();
                for (var entry : dispatchIndexMethods.entrySet()) {
                    var list = entry.getValue();
                    if (!seenIndexMethods.add(Objects.toIdentityString(list)))
                        continue;

                    for (var entry2 : list.reversed()) {
                        var lastLuaTypeName = this.luaTypeName;
                        luaTypeName = entry2.getValue();

                        index.addStatement("case $T self1 -> $L(state, self1, atom)",
                            entry2.getKey(), namespace("index$dispatch", false));
                        newindex.addStatement("case $T self1 -> $L(state, self1, atom)",
                            entry2.getKey(), namespace("newindex$dispatch", false));
                        namecall.addStatement("case $T self1 -> $L(state, self1, atom)",
                            entry2.getKey(), namespace("namecall$dispatch", false));

                        luaTypeName = lastLuaTypeName;
                    }
                }

                index.addStatement("default -> $L(state, self, atom)", namespace("index$dispatch", false));
                index.addCode("$<};");
                glueTypeBuilder.addMethod(index.build());
                newindex.addStatement("default -> $L(state, self, atom)", namespace("newindex$dispatch", false));
                newindex.addCode("$<};");
                glueTypeBuilder.addMethod(newindex.build());
                namecall.addStatement("default -> $L(state, self, atom)", namespace("namecall$dispatch", false));
                namecall.addCode("$<};");
                glueTypeBuilder.addMethod(namecall.build());

                luaTypeName = oldLuaTypeName;
            }

            return null;
        }

        var export = e.getAnnotation(LuaExport.class);
        if (export == null) return null; // Not exported, continue

        // We need to enter the inner type, process, then return

        var superType = ClassName.get(e.getSuperclass());
        if (superType.equals(TypeName.get(Object.class)) || superType.equals(TypeName.get(Record.class)))
            superType = null;
        boolean isFinal = e.getModifiers().contains(Modifier.FINAL);

        TypeName lastTypeName = typeName;
        typeName = TypeName.get(e.asType());
        boolean lastIsDispatch = isDispatch;
        isDispatch = superType != null || !isFinal;
        String lastLuaTypeName = luaTypeName;
        luaTypeName = e.getSimpleName().toString();
        MethodSpec.Builder lastIndexMethod = indexMethod;
        indexMethod = beginIndexMethod();
        MethodSpec.Builder lastNewIndexMethod = newIndexMethod;
        newIndexMethod = beginNewIndexMethod();
        MethodSpec.Builder lastNamecallMethod = namecallMethod;
        namecallMethod = beginNamecallMethod();

        enterNamedType(superType);

        super.visitType(e, unused);

        endNamedType(superType);

        glueTypeBuilder.addMethod(endNamecallMethod(superType));
        namecallMethod = lastNamecallMethod;
        glueTypeBuilder.addMethod(endNewIndexMethod(superType));
        newIndexMethod = lastNewIndexMethod;
        glueTypeBuilder.addMethod(endIndexMethod(superType));
        indexMethod = lastIndexMethod;
        luaTypeName = lastLuaTypeName;
        isDispatch = lastIsDispatch;
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

        boolean isGetter = isProperty && e.getSimpleName().toString().startsWith("get");
        boolean isSetter = isProperty && e.getSimpleName().toString().startsWith("set");

        if (isGetter) appendIndexCall(e, isStatic);
        else if (isSetter) appendNewIndexCall(e, isStatic);
        else if (isMethod) appendNamecallCall(e);
        else messager.printError("not namecall, getter, or setter isProperty=" +
                                 isProperty + ", name=" + e.getSimpleName().toString(), e);

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

    private void appendNewIndexCall(ExecutableElement e, boolean isStatic) {
        var javaName = e.getSimpleName().toString();
        var luaName = LuaNames.toLuaProperty(javaName);
        var isVoid = e.getReturnType().getKind() == TypeKind.VOID;

        newIndexMethod.addCode("case $L/*$L*/ -> ", atomizer.atomize(luaName), luaName);
        if (isVoid) newIndexMethod.beginControlFlow("");
        if (isStatic) {
            newIndexMethod.addStatement("$T.$L(state)",
                TypeName.get(e.getEnclosingElement().asType()), javaName);
        } else {
            newIndexMethod.addStatement("self.$L(state)", javaName);
        }
        if (isVoid) {
            newIndexMethod.addStatement("yield 0");
            newIndexMethod.endControlFlow();
        }
    }

    private void appendNamecallCall(ExecutableElement e) {
        var javaName = e.getSimpleName().toString();
        var luaName = LuaNames.toLuaMethod(javaName);
        var isVoid = e.getReturnType().getKind() == TypeKind.VOID;

        var methods = e.getAnnotationsByType(LuaMethod.class);
        if (methods.length > 0 && methods[0].meta() != Meta.NONE) {
            appendMetaMethod(e, methods[0].meta());
            return;
        }

        if (luaTypeName == null) {
            if (isVoid) {
                // Doesnt work because we return the function reference. We could generate
                // a wrapper function here but i am lazy :-)
                messager.printError("Cannot export void methods from library classes", e);
                return;
            }

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
            if (isVoid) {
                namecallMethod.addStatement("self.$L(state)", javaName);
                namecallMethod.addStatement("yield 0");
            } else {
                namecallMethod.addStatement("yield self.$L(state)", javaName);
            }
            namecallMethod.endControlFlow();
        }
    }

    private void appendMetaMethod(ExecutableElement e, Meta meta) {
        var name = e.getSimpleName().toString();
        var glueName = namespace(name, false);
        var isVoid = e.getReturnType().getKind() == TypeKind.VOID;

        if (isDispatch && !dispatchTypes.contains(TypeName.get(e.getEnclosingElement().asType()))) {
            messager.printError("Only base classes may define metamethods", e);
            return;
        }

        // create the accessor
        createLuaFunc(namespace(name, true), glueName, meta.methodName());

        // create the wrapper method
        var wrapper = MethodSpec.methodBuilder(glueName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        wrapper.addStatement("$T self = check$LArg(state, 1)", typeName, luaTypeName);
        wrapper.addStatement("state.remove(1)"); // remove self arg (for nicer syntax)

        if (!isVoid) wrapper.addCode("return ");
        wrapper.addStatement("self.$L(state)", name);
        if (isVoid) wrapper.addStatement("return 0");
        glueTypeBuilder.addMethod(wrapper.build());

        // register in metatable
        registerMethod.addStatement("state.pushFunction($L)", namespace(name, true));
        registerMethod.addStatement("state.setField(-2, $S)", meta.methodName());
    }

    private MethodSpec.Builder beginRegisterMethod() {
        var method = MethodSpec.methodBuilder("register")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(LuaState.class, "state");

        {   // Module value setup
            // For the library object we dont actually care about the
            // userdata itself, none of the 'static' functions use it.
            method.addStatement("state.newUserData(new Object())");
            method.addStatement("state.newMetaTable(LIB_NAME)"); // metatable

            method.addStatement("state.pushString(LIB_NAME)");
            method.addStatement("state.setField(-2, $S)", "__type");
            method.addStatement("state.pushFunction(INDEX)");
            method.addStatement("state.setField(-2, $S)", LuaNames.INDEX_META_NAME);
            method.addStatement("state.pushFunction(NEWINDEX)");
            method.addStatement("state.setField(-2, $S)", LuaNames.NEWINDEX_META_NAME);
            method.addStatement("state.pushFunction(NAMECALL)");
            method.addStatement("state.setField(-2, $S)", LuaNames.NAMECALL_META_NAME);

            method.addStatement("state.setReadOnly(-1, true)"); // metatable read only
            method.addStatement("state.setMetaTable(-2)"); // assign metatable to userdata

            if (isGlobal) {
                method.addStatement("state.setGlobal(LIB_NAME)");
            } else {
                method.addStatement("state.requireRegisterModule(LIB_NAME)"); // register for require
                // userdata is popped during register.
            }
        }

        return method;
    }

    private MethodSpec endRegisterMethod() {
        return registerMethod.build();
    }

    private MethodSpec.Builder beginIndexMethod() {
        var glueName = namespace(isDispatch ? "index$dispatch" : "index$meta", false);

        var method = MethodSpec.methodBuilder(glueName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);

        if (isDispatch) {
            // For inheriting classes, we take self and atom as an input
            method.addParameter(typeName, "self");
            method.addParameter(int.class, "atom");
        } else {
            // Create the accessor
            createLuaFunc(namespace("INDEX", true), glueName, LuaNames.INDEX_META_NAME);

            // If member, need to add read for self parameter
            if (luaTypeName != null) {
                method.addStatement("$T self = check$LArg(state, 1)", typeName, luaTypeName);
            }

            method.addStatement("short atom = state.toStringAtomRaw(2)");
        }

        method.beginControlFlow("return switch (atom)");
        return method;
    }

    private MethodSpec endIndexMethod(@Nullable TypeName superType) {
        // There are three remaining scenarios here:
        // 1. the atom is NO_ATOM in which case we dont need to do anything ever
        indexMethod.addStatement("case $T.NO_ATOM -> 0", LuaState.class);

        if (superType != null) {
            // 2. if we have a superclass, delegate to that in default
            var oldLuaTypeName = luaTypeName;
            luaTypeName = ((ClassName) superType).simpleName();

            indexMethod.addStatement("default -> $L(state, self, atom)", namespace("index$dispatch", false));

            luaTypeName = oldLuaTypeName;
        } else {
            // 3. no superclass, just exit
            indexMethod.addStatement("default -> 0");
        }

        indexMethod.addCode("$<};");
        return indexMethod.build();
    }

    private MethodSpec.Builder beginNewIndexMethod() {
        var glueName = namespace(isDispatch ? "newindex$dispatch" : "newindex$meta", false);

        var method = MethodSpec.methodBuilder(glueName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        if (isDispatch) {
            // For inheriting classes, we take self and namecall atom as an input
            method.addParameter(typeName, "self");
            method.addParameter(int.class, "atom");
        } else {
            // Create the accessor
            createLuaFunc(namespace("NEWINDEX", true), glueName, LuaNames.NEWINDEX_META_NAME);

            // If member, need to add read for self parameter
            if (luaTypeName != null)
                method.addStatement("$T self = check$LArg(state, 1)", typeName, luaTypeName);
            method.addStatement("state.remove(1)"); // remove self arg (for nicer syntax)

            method.addStatement("short atom = state.toStringAtomRaw(1)");
            method.addStatement("state.remove(1)"); // remove self arg (for nicer syntax)
        }

        method.beginControlFlow("return switch (atom)");

        return method;
    }

    private MethodSpec endNewIndexMethod(@Nullable TypeName superType) {

        // TODO: this exists to prevent java from getting mad about having no result expression cases
        //       from a switch expression. Instead we should just not generate the switch if there are
        //       no methods on a given type.
        newIndexMethod.addStatement("case Short.MIN_VALUE -> 0");

        if (superType != null) {
            var oldLuaTypeName = luaTypeName;
            luaTypeName = ((ClassName) superType).simpleName();

            newIndexMethod.addStatement("default -> $L(state, self, atom)", namespace("newindex$dispatch", false));

            luaTypeName = oldLuaTypeName;
        } else {
            newIndexMethod.addStatement("default -> throw state.error($S)", "Attempt to update nonexistent property");
        }
        newIndexMethod.addCode("$<};");

        return newIndexMethod.build();
    }

    private MethodSpec.Builder beginNamecallMethod() {
        var glueName = namespace(isDispatch ? "namecall$dispatch" : "namecall$meta", false);

        var method = MethodSpec.methodBuilder(glueName)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter(LuaState.class, "state")
            .returns(int.class);
        if (isDispatch) {
            // For inheriting classes, we take self and namecall atom as an input
            method.addParameter(typeName, "self");
            method.addParameter(int.class, "atom");
        } else {
            // Create the accessor
            createLuaFunc(namespace("NAMECALL", true), glueName, LuaNames.NAMECALL_META_NAME);

            // If member, need to add read for self parameter
            if (luaTypeName != null) {
                method.addStatement("$T self = check$LArg(state, 1)", typeName, luaTypeName);
            }

            method.addStatement("short atom = state.nameCallAtomRaw()");
        }

        method.beginControlFlow("return switch (atom)");

        return method;
    }

    private MethodSpec endNamecallMethod(@Nullable TypeName superType) {

        // TODO: this exists to prevent java from getting mad about having no result expression cases
        //       from a switch expression. Instead we should just not generate the switch if there are
        //       no methods on a given type.
        namecallMethod.addStatement("case Short.MIN_VALUE -> 0");

        if (superType != null) {
            var oldLuaTypeName = luaTypeName;
            luaTypeName = ((ClassName) superType).simpleName();

            namecallMethod.addStatement("default -> $L(state, self, atom)", namespace("namecall$dispatch", false));

            luaTypeName = oldLuaTypeName;
        } else {
            namecallMethod.addStatement("default -> throw state.error($S)", "Attempt to call nonexistent method");
        }
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

    private void enterNamedType(@Nullable TypeName superType) {
        if (isDispatch && superType != null) {
            //todo rename me
            var dispatchedIndex = dispatchIndexMethods.get(superType);
            dispatchIndexMethods.put(typeName, dispatchedIndex);
            dispatchedIndex.add(Map.entry(typeName, luaTypeName));
            return;
        }
        if (isDispatch) {
            dispatchTypes.add(typeName);
            dispatchIndexMethods.put(typeName, new ArrayList<>());
        }

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

        registerMethod.addStatement("state.newMetaTable(LIB_NAME + $S)", "." + luaTypeName); // metatable

        registerMethod.addStatement("state.pushString($S)", luaTypeName);
        registerMethod.addStatement("state.setField(-2, $S)", "__type");
        registerMethod.addStatement("state.pushFunction($L)", namespace("INDEX", true));
        registerMethod.addStatement("state.setField(-2, $S)", LuaNames.INDEX_META_NAME);
        registerMethod.addStatement("state.pushFunction($L)", namespace("NEWINDEX", true));
        registerMethod.addStatement("state.setField(-2, $S)", LuaNames.NEWINDEX_META_NAME);
        registerMethod.addStatement("state.pushFunction($L)", namespace("NAMECALL", true));
        registerMethod.addStatement("state.setField(-2, $S)", LuaNames.NAMECALL_META_NAME);
    }

    private void endNamedType(@Nullable TypeName superType) {
        if (isDispatch && superType != null) return;

        var tagName = namespace("TAG", true);

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
