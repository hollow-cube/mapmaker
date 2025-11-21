package net.hollowcube.luau.slopgen;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;
import com.sun.source.util.DocTrees;
import net.hollowcube.luau.gen.LuaLibrary;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedOptions({"org.gradle.annotation.processing.aggregating"})
public class LuaSlopgenProcessor extends AbstractProcessor {
    // This has to be an aggregating processor currently because we need to create
    // the string atom table, however it could be possible to instead run two processors
    // one which is isolating for each file and reference atoms like `LuaStringAtoms.MyString`
    // which will be inlined during compiltion
    //
    // However, an aggregating processor is still needed to generate LuaStringAtoms, and
    // its not obvious how to communicate the strings from the first step.
    // It also isnt a performance issue currently, so dont care.

    private final StringAtomizer atomizer = new StringAtomizer();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        var elementUtils = processingEnv.getElementUtils();
        var filer = processingEnv.getFiler();
        var docTrees = DocTrees.instance(processingEnv);

        for (var annotatedElement : roundEnv.getElementsAnnotatedWith(LuaLibrary.class)) {
            if (!(annotatedElement instanceof TypeElement typeElement)) continue;

            if (!typeElement.getModifiers().contains(Modifier.FINAL))
                messager.printError("LuaLibrary classes must be final", annotatedElement);

            var luaLibraryMirror = typeElement.getAnnotationMirrors().stream()
                .filter(mirror -> mirror.getAnnotationType().toString().equals(LuaLibrary.class.getName()))
                .findFirst().orElseThrow();
            var luaLibraryValues = luaLibraryMirror.getElementValues().entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().getSimpleName().toString(),
                Map.Entry::getValue));

            var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            var glueTypeName = ClassName.get(packageName, typeElement.getSimpleName() + "$luau");
            var glueTypeBuilder = TypeSpec.classBuilder(glueTypeName)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addOriginatingElement(annotatedElement);

            glueTypeBuilder.addField(FieldSpec.builder(String.class, "LIB_NAME")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                .initializer("$S", luaLibraryValues.get("name").getValue().toString())
                .build());

            var builder = new LuaLibraryClassBuildingVisitor(
                processingEnv, atomizer, TypeName.get(typeElement.asType()), glueTypeName, glueTypeBuilder);
            builder.visit(typeElement);
            builder.finish();

//            var superType = ClassName.get(typeElement.getSuperclass());
//            if (superType.equals(TypeName.get(Object.class))) superType = null;
//            var baseType = getBaseType(processingEnv.getTypeUtils(), typeElement);
//
//            TypeName glueSuperType = null;
//            if (superType != null) {
//                glueSuperType = ClassName.get(packageName, ((ClassName) superType).simpleName() + "$luau");
//                glueTypeBuilder.addSuperinterface(glueSuperType);
//            }
//
//            var annotatedType = TypeName.get(typeElement.asType());
//            var targetType = luaTypeMirrorValues.containsKey("implFor")
//                ? TypeName.get((TypeMirror) luaTypeMirrorValues.get("implFor").getValue())
//                : annotatedType;
//            var targetName = luaTypeMirrorValues.containsKey("name")
//                ? (String) luaTypeMirrorValues.get("name").getValue()
//                : typeElement.getSimpleName().toString().replace("Lua", "");
//
//            var luaHelpersType = ClassName.get("net.hollowcube.mapmaker.runtime.freeform.script", "LuaHelpers");
//
//            var needsMetaProxies = targetType.equals(annotatedType);
//
//            var handles = new ArrayList<LuaHandle>();
//            new LuaHandleCollector(messager, docTrees).visit(typeElement, handles);
//            var getterSetterNames = handles.stream()
//                .filter(h -> h.metaType() == null && !h.isLuaStatic() && h.isProperty())
//                .filter(h -> h.methodName().startsWith("get") || h.methodName().startsWith("set"))
//                .map(LuaHandle::methodName)
//                .toList();
//
//            // Add constant with metatable/type name
//            if (superType == null) {
//                glueTypeBuilder.addField(FieldSpec.builder(String.class, "TYPE_NAME",
//                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
//                    .initializer("$S", targetName)
//                    .build());
//            }
//
//            boolean foundEqImpl = false, foundToStringImpl = false;
//
//            // Init Method
//            if (superType == null) {
//                var initMethod = MethodSpec.methodBuilder("init$luau")
//                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//                    .addParameter(LuaState.class, "state")
//                    .returns(TypeName.VOID);
//
//                initMethod.addStatement("state.newMetaTable(TYPE_NAME)");
//                initMethod.addStatement("state.pushString(TYPE_NAME)");
//                initMethod.addStatement("state.setField(-2, $S)", "__type");
//
//                // Insert references to meta methods
//                for (var handle : handles) {
//                    if (handle.metaType() == null || handle.isLuaStatic())
//                        continue;
//                    // Index, newindex, and namecall are always proxied through the impl class. Underlying implementations can still
//                    // implement these functions, but they will be the "default" case if no other match is found.
//                    if (handle.metaType() == MetaType.INDEX || handle.metaType() == MetaType.NEWINDEX || handle.metaType() == MetaType.NAMECALL)
//                        continue;
//
//                    foundEqImpl |= handle.metaType() == MetaType.EQ;
//                    foundToStringImpl |= handle.metaType() == MetaType.TOSTRING;
//
//                    initMethod.addStatement("state.pushCFunction($T::$L, $S)", handle.owningType(),
//                        handle.methodName(), handle.metaType().methodName());
//                    initMethod.addStatement("state.setField(-2, $S)", handle.metaType().methodName());
//                }
//
//                // If we didn't find __eq or __tostring, add the default impl
//                if (!foundEqImpl) {
//                    initMethod.addStatement("state.pushCFunction($T::luaEq, $S)", glueTypeName, "__eq");
//                    initMethod.addStatement("state.setField(-2, $S)", "__eq");
//                }
//                if (!foundToStringImpl) {
//                    initMethod.addStatement("state.pushCFunction($T::luaToString, $S)", glueTypeName, "__tostring");
//                    initMethod.addStatement("state.setField(-2, $S)", "__tostring");
//                }
//                // Always add __index, __newindex, __namecall to the glue implementation
//                initMethod.addStatement("state.pushCFunction($T::$L, $S)", glueTypeName,
//                    needsMetaProxies ? "luaIndex$proxy" : "luaIndex", "__index");
//                initMethod.addStatement("state.setField(-2, $S)", "__index");
//                initMethod.addStatement("state.pushCFunction($T::$L, $S)", glueTypeName,
//                    needsMetaProxies ? "luaNewIndex$proxy" : "luaNewIndex", "__newindex");
//                initMethod.addStatement("state.setField(-2, $S)", "__newindex");
//                initMethod.addStatement("state.pushCFunction($T::$L, $S)", glueTypeName,
//                    needsMetaProxies ? "luaNameCall$proxy" : "luaNameCall", "__namecall");
//                initMethod.addStatement("state.setField(-2, $S)", "__namecall");
//
//                initMethod.addStatement("state.pop(1)"); // Pop the metatable
//
//                initMethod.addCode("\n");
//
//                initMethod.addStatement("state.newTable()");
//                initMethod.addStatement("state.pushValue(-1)");
//                initMethod.addStatement("state.setMetaTable(-2)"); // Metatable to itself
//
//                // Insert references to 'static' methods
//                for (var handle : handles) {
//                    if (!handle.isLuaStatic())
//                        continue;
//
//                    var methodName = handle.metaType() != null ? handle.metaType().methodName() : handle.methodName();
//                    if (methodName.endsWith("_")) methodName = methodName.substring(0, methodName.length() - 1);
//                    initMethod.addStatement("state.pushCFunction($T::$L, $S)", handle.owningType(), handle.methodName(), methodName);
//                    initMethod.addStatement("state.setField(-2, $S)", methodName);
//                }
//
//                initMethod.addStatement("state.setReadOnly(-1, true)");
//                initMethod.addStatement("state.setGlobal(TYPE_NAME)");
//
//                glueTypeBuilder.addMethod(initMethod.build());
//
//                // Insert the proxies for luaIndex, luaNewIndex, luaNameCall if not using a type impl
//                if (needsMetaProxies) {
//                    for (var proxy : List.of("luaIndex", "luaNewIndex", "luaNameCall")) {
//                        var method = MethodSpec.methodBuilder(proxy + "$proxy")
//                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//                            .addParameter(LuaState.class, "state")
//                            .returns(TypeName.INT);
//                        addCheck(method, "self", 1, targetType, annotatedType, superType, baseType);
//                        method.addStatement("return (($T) self).$L(state)", glueTypeName, proxy);
//                        glueTypeBuilder.addMethod(method.build());
//                    }
//                }
//            }
//
//            // If we didnt find eq or toString impls, add the defaults
//            if (superType == null && !foundEqImpl) {
//                var method = MethodSpec.methodBuilder("luaEq")
//                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//                    .addParameter(LuaState.class, "state")
//                    .returns(TypeName.INT);
//                addCheck(method, "lhs", 1, targetType, annotatedType, superType, baseType);
//                addCheck(method, "rhs", 2, targetType, annotatedType, superType, baseType);
//                method.addStatement("state.pushBoolean($T.equals(lhs, rhs))", Objects.class)
//                    .addStatement("return 1");
//                glueTypeBuilder.addMethod(method.build());
//            }
//            if (superType == null && !foundToStringImpl) {
//                var method = MethodSpec.methodBuilder("luaToString")
//                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//                    .addParameter(LuaState.class, "state")
//                    .returns(TypeName.INT);
//                addCheck(method, "obj", 1, targetType, annotatedType, superType, baseType);
//                method.addStatement("state.pushString($T.toString(obj))", Objects.class)
//                    .addStatement("return 1");
//
//                glueTypeBuilder.addMethod(method.build());
//            }
//
//            {   // Generate __index metamethod impl
//                var indexMethod = MethodSpec.methodBuilder("luaIndex")
//                    .addModifiers(Modifier.PUBLIC, needsMetaProxies ? Modifier.DEFAULT : Modifier.STATIC)
//                    .addParameter(LuaState.class, "state")
//                    .returns(TypeName.INT);
//
//                addCheck(indexMethod, "self", 1, targetType, annotatedType, superType, baseType);
//                indexMethod.addStatement("$T key = state.checkStringArg(2)", String.class);
//                indexMethod.beginControlFlow("return switch (key)");
//
//                for (var method : handles) {
//                    if (method.metaType() != null || method.isLuaStatic() || !method.isProperty())
//                        continue;
//                    if (method.methodName().startsWith("set")) {
//                        // Check for read-only properties
//                        var hasGetter = getterSetterNames.contains("get" + method.methodName().substring(3));
//                        if (!hasGetter) {
//                            indexMethod.addStatement("case $S -> $T.fieldWriteOnly(state, TYPE_NAME, key)",
//                                method.methodName().substring(3), luaHelpersType);
//                        }
//                        continue;
//                    }
//                    if (!method.methodName().startsWith("get"))
//                        continue;
//
//                    indexMethod.addCode("case $S -> ", method.methodName().substring(3));
//                    if (method.isStatic()) {
//                        indexMethod.addStatement("$T.$L(state)", method.owningType(), method.methodName());
//                    } else {
//                        indexMethod.addStatement("self.$L(state)", method.methodName());
//                    }
//                }
//
//                // If the class provides its own index metamethod, call that as the default case
//                // Only search for the proxy in the base class, otherwise we call the super as the default case.
//                boolean foundIndexProxy = false;
//                if (superType == null) {
//                    for (var method : handles) {
//                        if (method.metaType() != MetaType.INDEX || method.isLuaStatic())
//                            continue;
//
//                        foundIndexProxy = true;
//                        indexMethod.addCode("default -> ");
//                        if (method.isStatic()) {
//                            indexMethod.addStatement("$T.$L(state)", method.owningType(), method.methodName());
//                        } else {
//                            indexMethod.addStatement("self.$L(state)", method.methodName());
//                        }
//                    }
//                }
//                if (superType != null) {
//                    indexMethod.addStatement("default -> $T.super.luaIndex(state)", glueSuperType);
//                } else if (!foundIndexProxy) {
//                    indexMethod.addStatement("default -> $T.noSuchKey(state, TYPE_NAME, key)", luaHelpersType);
//                }
//
//                indexMethod.addCode("$<};");
//                glueTypeBuilder.addMethod(indexMethod.build());
//            }
//            {   // Generate __newindex metamethod impl
//                var newIndexMethod = MethodSpec.methodBuilder("luaNewIndex")
//                    .addModifiers(Modifier.PUBLIC, needsMetaProxies ? Modifier.DEFAULT : Modifier.STATIC)
//                    .addParameter(LuaState.class, "state")
//                    .returns(TypeName.INT);
//
//                addCheck(newIndexMethod, "self", 1, targetType, annotatedType, superType, baseType);
//                newIndexMethod.addStatement("$T key = state.checkStringArg(2)", String.class);
//                newIndexMethod.beginControlFlow("return switch (key)");
//
//                for (var method : handles) {
//                    if (method.metaType() != null || method.isLuaStatic() || !method.isProperty())
//                        continue;
//                    if (method.methodName().startsWith("get")) {
//                        // Check for read-only properties
//                        var hasSetter = getterSetterNames.contains("set" + method.methodName().substring(3));
//                        if (!hasSetter) {
//                            newIndexMethod.addStatement("case $S -> $T.fieldReadOnly(state, TYPE_NAME, key)",
//                                method.methodName().substring(3), luaHelpersType);
//                        }
//                        continue;
//                    }
//                    if (!method.methodName().startsWith("set"))
//                        continue;
//
//                    newIndexMethod.addCode("case $S -> ", method.methodName().substring(3));
//                    if (method.isStatic()) {
//                        newIndexMethod.addStatement("$T.$L(state)", method.owningType(), method.methodName());
//                    } else {
//                        // In the non-static case we remove the self and key args from the stack so the first arg
//                        // is the key being set for setter methods.
//                        newIndexMethod.addCode("{$>\n");
//                        newIndexMethod.addStatement("state.remove(1)");
//                        newIndexMethod.addStatement("state.remove(1)");
//                        newIndexMethod.addStatement("yield self.$L(state)", method.methodName());
//                        newIndexMethod.addCode("$<}\n");
//                    }
//                }
//
//                // If the class provides its own index metamethod, call that as the default case
//                // Only search for the proxy in the base class, otherwise we call the super as the default case.
//                boolean foundNewIndexProxy = false;
//                if (superType == null) {
//                    for (var method : handles) {
//                        if (method.metaType() != MetaType.NEWINDEX || method.isLuaStatic())
//                            continue;
//
//                        foundNewIndexProxy = true;
//                        newIndexMethod.addCode("default -> ");
//                        if (method.isStatic()) {
//                            newIndexMethod.addStatement("$T.$L(state)", method.owningType(), method.methodName());
//                        } else {
//                            newIndexMethod.addStatement("self.$L(state)", method.methodName());
//                        }
//                    }
//                }
//                if (superType != null) {
//                    newIndexMethod.addStatement("default -> $T.super.luaNewIndex(state)", glueSuperType);
//                } else if (!foundNewIndexProxy) {
//                    newIndexMethod.addStatement("default -> $T.noSuchKey(state, TYPE_NAME, key)", luaHelpersType);
//                }
//
//                newIndexMethod.addCode("$<};");
//                glueTypeBuilder.addMethod(newIndexMethod.build());
//            }
//            {   // Generate __namecall metamethod impl
//                var nameCallMethod = MethodSpec.methodBuilder("luaNameCall")
//                    .addModifiers(Modifier.PUBLIC, needsMetaProxies ? Modifier.DEFAULT : Modifier.STATIC)
//                    .addParameter(LuaState.class, "state")
//                    .returns(TypeName.INT);
//
//                addCheck(nameCallMethod, "self", 1, targetType, annotatedType, superType, baseType);
//                nameCallMethod.addStatement("$T methodName = state.nameCallAtom()", String.class);
//                nameCallMethod.beginControlFlow("return switch (methodName)");
//
//                // state.remove(1); // Remove the world userdata from the stack (so implementations can pretend they have no self)
//                for (var method : handles) {
//                    if (method.metaType() != null || method.isLuaStatic() || method.isProperty())
//                        continue;
//
//                    nameCallMethod.addCode("case $S -> ", method.methodName().substring(0, 1).toUpperCase(Locale.ROOT) + method.methodName().substring(1));
//                    if (method.isStatic()) {
//                        nameCallMethod.addStatement("$T.$L(state)", method.owningType(), method.methodName());
//                    } else {
//                        // In the non-static case we remove the self arg from the stack so the first arg
//                        // is the first parameter to the method.
//                        nameCallMethod.addCode("{$>\n");
//                        nameCallMethod.addStatement("state.remove(1)");
//                        nameCallMethod.addStatement("yield self.$L(state)", method.methodName());
//                        nameCallMethod.addCode("$<}\n");
//                    }
//                }
//
//                // If the class provides its own namecall metamethod, call that as the default case
//                // Only search for the proxy in the base class, otherwise we call the super as the default case.
//                boolean foundNameCallProxy = false;
//                if (superType == null) {
//                    for (var method : handles) {
//                        if (method.metaType() != MetaType.NAMECALL || method.isLuaStatic())
//                            continue;
//
//                        foundNameCallProxy = true;
//                        nameCallMethod.addCode("default -> ");
//                        if (method.isStatic()) {
//                            nameCallMethod.addStatement("$T.$L(state)", method.owningType(), method.methodName());
//                        } else {
//                            nameCallMethod.addStatement("self.$L(state)", method.methodName());
//                        }
//                    }
//                }
//                if (superType != null) {
//                    nameCallMethod.addStatement("default -> $T.super.luaNameCall(state)", glueSuperType);
//                } else if (!foundNameCallProxy) {
//                    nameCallMethod.addStatement("default -> $T.noSuchMethod(state, TYPE_NAME, methodName)", luaHelpersType);
//                }
//
//                nameCallMethod.addCode("$<};");
//                glueTypeBuilder.addMethod(nameCallMethod.build());
//            }

            try {
                JavaFile.builder(packageName, glueTypeBuilder.build())
                    .addFileComment("Generated by Lua Slopgen. DO NOT EDIT!")
                    .indent("    ")
                    .build()
                    .writeTo(filer);
            } catch (IOException e) {
                messager.printError("Failed to write generated file for " + annotatedElement.getSimpleName(), annotatedElement);
            }
        }

        if (roundEnv.processingOver()) {
            try {
                atomizer.build().writeTo(filer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(LuaLibrary.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_25;
    }
}
