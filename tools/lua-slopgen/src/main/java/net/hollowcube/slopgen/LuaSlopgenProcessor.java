package net.hollowcube.slopgen;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.*;
import com.sun.source.util.DocTrees;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaType;
import net.hollowcube.luau.annotation.MetaType;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@AutoService(Processor.class)
public class LuaSlopgenProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        var elementUtils = processingEnv.getElementUtils();
        var filer = processingEnv.getFiler();
        var docTrees = DocTrees.instance(processingEnv);

        for (var annotatedElement : roundEnv.getElementsAnnotatedWith(LuaType.class)) {
            if (!(annotatedElement instanceof TypeElement typeElement)) continue;

            var luaTypeMirror = typeElement.getAnnotationMirrors().stream()
                    .filter(mirror -> mirror.getAnnotationType().toString().equals(LuaType.class.getName()))
                    .findFirst().orElseThrow();
            var luaTypeMirrorValues = luaTypeMirror.getElementValues().entrySet().stream().collect(Collectors.toMap(
                    e -> e.getKey().getSimpleName().toString(),
                    Map.Entry::getValue));

            var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            var glueTypeName = ClassName.get(packageName, typeElement.getSimpleName() + "$luau");
            var glueTypeBuilder = TypeSpec.interfaceBuilder(glueTypeName);

            var annotatedType = TypeName.get(typeElement.asType());
            var targetType = luaTypeMirrorValues.containsKey("implFor")
                    ? (TypeMirror) luaTypeMirrorValues.get("implFor").getValue()
                    : annotatedType;
            var targetName = luaTypeMirrorValues.containsKey("name")
                    ? (String) luaTypeMirrorValues.get("name").getValue()
                    : typeElement.getSimpleName().toString().replace("Lua", "");

            var handles = new ArrayList<LuaHandle>();
            new LuaHandleCollector(messager, docTrees).visit(typeElement, handles);

            // Add constant with metatable/type name
            glueTypeBuilder.addField(FieldSpec.builder(String.class, "TYPE_NAME",
                            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$S", targetName)
                    .build());

            boolean foundEqImpl = false, foundToStringImpl = false;

            {   // Init Method
                var initMethod = MethodSpec.methodBuilder("init$luau")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(LuaState.class, "state")
                        .returns(TypeName.VOID);

                initMethod.addStatement("state.newMetaTable(TYPE_NAME)");
                initMethod.addStatement("state.pushString(TYPE_NAME)");
                initMethod.addStatement("state.setField(-2, $S)", "__type");

                // Insert references to meta methods
                for (var handle : handles) {
                    if (handle.metaType() == null || handle.isLuaStatic())
                        continue;
                    // Index, newindex, and namecall are always proxied through the impl class. Underlying implementations can still
                    // implement these functions, but they will be the "default" case if no other match is found.
                    if (handle.metaType() == MetaType.INDEX || handle.metaType() == MetaType.NEWINDEX || handle.metaType() == MetaType.NAMECALL)
                        continue;

                    foundEqImpl |= handle.metaType() == MetaType.EQ;
                    foundToStringImpl |= handle.metaType() == MetaType.TOSTRING;

                    initMethod.addStatement("state.pushCFunction($T::$L, $S)", handle.owningType(),
                            handle.methodName(), handle.metaType().methodName());
                    initMethod.addStatement("state.setField(-2, $S)", handle.metaType().methodName());
                }

                // If we didn't find __eq or __tostring, add the default impl
                if (!foundEqImpl) {
                    initMethod.addStatement("state.pushCFunction($T::luaEq, $S)", glueTypeName, "__eq");
                    initMethod.addStatement("state.setField(-2, $S)", "__eq");
                }
                if (!foundToStringImpl) {
                    initMethod.addStatement("state.pushCFunction($T::luaToString, $S)", glueTypeName, "__tostring");
                    initMethod.addStatement("state.setField(-2, $S)", "__tostring");
                }
                // Always add __index, __newindex, __namecall to the glue implementation
                initMethod.addStatement("state.pushCFunction($T::luaIndex, $S)", glueTypeName, "__index");
                initMethod.addStatement("state.setField(-2, $S)", "__index");
                initMethod.addStatement("state.pushCFunction($T::luaNewIndex, $S)", glueTypeName, "__newindex");
                initMethod.addStatement("state.setField(-2, $S)", "__newindex");
                initMethod.addStatement("state.pushCFunction($T::luaNameCall, $S)", glueTypeName, "__namecall");
                initMethod.addStatement("state.setField(-2, $S)", "__namecall");

                initMethod.addStatement("state.pop(1)"); // Pop the metatable

                initMethod.addCode("\n");

                initMethod.addStatement("state.newTable()");
                initMethod.addStatement("state.pushValue(-1)");
                initMethod.addStatement("state.setMetaTable(-2)"); // Metatable to itself

                // Insert references to 'static' methods
                for (var handle : handles) {
                    if (!handle.isLuaStatic())
                        continue;

                    var methodName = handle.metaType() != null ? handle.metaType().methodName() : handle.methodName();
                    if (methodName.endsWith("_")) methodName = methodName.substring(0, methodName.length() - 1);
                    initMethod.addStatement("state.pushCFunction($T::$L, $S)", handle.owningType(), handle.methodName(), methodName);
                    initMethod.addStatement("state.setField(-2, $S)", methodName);
                }

                initMethod.addStatement("state.setReadOnly(-1, true)");
                initMethod.addStatement("state.setGlobal(TYPE_NAME)");

                glueTypeBuilder.addMethod(initMethod.build());
            }

            // If we didnt find eq or toString impls, add the defaults
            if (!foundEqImpl) {
                glueTypeBuilder.addMethod(MethodSpec.methodBuilder("luaEq")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(LuaState.class, "state")
                        .returns(TypeName.INT)
                        .addStatement("$T lhs = $T.checkArg(state, 1)", targetType, annotatedType)
                        .addStatement("$T rhs = $T.checkArg(state, 2)", targetType, annotatedType)
                        .addStatement("state.pushBoolean($T.equals(lhs, rhs))", Objects.class)
                        .addStatement("return 1")
                        .build());
            }
            if (!foundToStringImpl) {
                glueTypeBuilder.addMethod(MethodSpec.methodBuilder("luaToString")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(LuaState.class, "state")
                        .returns(TypeName.INT)
                        .addStatement("$T obj = $T.checkArg(state, 1)", targetType, annotatedType)
                        .addStatement("state.pushString($T.toString(obj))", Objects.class)
                        .addStatement("return 1")
                        .build());
            }

            // Always generate __index, __newindex, and __namecall.
            glueTypeBuilder.addMethod(MethodSpec.methodBuilder("luaIndex")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(LuaState.class, "state")
                    .returns(TypeName.INT)
                    .addStatement("state.error($S)", "Not implemented")
                    .addStatement("return 0")
                    .build());
            glueTypeBuilder.addMethod(MethodSpec.methodBuilder("luaNewIndex")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(LuaState.class, "state")
                    .returns(TypeName.INT)
                    .addStatement("state.error($S)", "Not implemented")
                    .addStatement("return 0")
                    .build());
            glueTypeBuilder.addMethod(MethodSpec.methodBuilder("luaNameCall")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(LuaState.class, "state")
                    .returns(TypeName.INT)
                    .addStatement("state.error($S)", "Not implemented")
                    .addStatement("return 0")
                    .build());

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

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(LuaType.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_25;
    }
}
