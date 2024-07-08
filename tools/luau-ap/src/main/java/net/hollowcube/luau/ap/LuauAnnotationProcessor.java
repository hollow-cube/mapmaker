package net.hollowcube.luau.ap;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.*;

@SupportedAnnotationTypes("net.hollowcube.luau.annotation.LuaObject")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class LuauAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;

    private TypeElement luaProperty;
    private TypeElement luaMethod;

    private TypeElement luaTypeImpl;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.elementUtils = processingEnvironment.getElementUtils();

        this.luaProperty = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaProperty"), "LuaProperty");
        this.luaMethod = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaMethod"), "LuaMethod");

        this.luaTypeImpl = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaTypeImpl"), "LuaTypeImpl");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Find available external type converters to include in processing.
        Map<TypeName, TypeConverter> typeConverters = new HashMap<>(TypeConverter.CONVERTER_MAP);
        for (var elem : roundEnv.getElementsAnnotatedWith(luaTypeImpl)) {
            var implType = TypeName.get(elem.asType());
            var anno = elem.getAnnotationMirrors().stream()
                    .filter(a -> a.getAnnotationType().asElement().equals(luaTypeImpl))
                    .findFirst().orElseThrow();
            var targetTypeMirror = anno.getElementValues().entrySet().stream()
                    .filter(e -> e.getKey().getSimpleName().contentEquals("value"))
                    .map(Map.Entry::getValue)
                    .map(v -> (TypeMirror) v.getValue())
                    .findFirst().orElseThrow();
            var targetType = TypeName.get(targetTypeMirror);
            typeConverters.put(targetType, new TypeConverter() {
                @Override
                public void insertPush(@NotNull MethodSpec.Builder method, @NotNull String getter) {
                    method.addStatement("$T.pushValue(state, $L)", implType, getter);
                }

                @Override
                public void insertPop(@NotNull MethodSpec.Builder method, @NotNull String name, int index) {
                    method.addStatement("$T $L = $T.checkArg(state, $L)", targetType, name, implType, index);
                }
            });
        }

        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElems = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element elem : annotatedElems) {
                var typeElem = (TypeElement) elem;
                if (!"java.lang.Object".equals(typeElem.getSuperclass().toString())) {
                    error(elem, "LuaObject must not have a superclass");
                    continue;
                }

                var packageName = elementUtils.getPackageOf(typeElem).getQualifiedName().toString();
                var wrappedClass = TypeName.get(typeElem.asType());
                if (wrappedClass instanceof ParameterizedTypeName ptn) {
                    wrappedClass = ptn.rawType;
                }
                var className = typeElem.getSimpleName().toString() + "$Wrapper";

                TypeSpec.Builder wrapper = TypeSpec.classBuilder(className)
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

                // Find the @LuaProperty, @LuaMethod declarations
                var properties = PropertyList.collect(
                        processingEnv.getMessager(), typeConverters,
                        getAnnotatedMembers(typeElem, luaProperty)
                );
                var methods = MethodList.collect(
                        processingEnv.getMessager(), typeConverters,
                        getAnnotatedMembers(typeElem, luaMethod)
                );

                // Gen a constant for the metatable name
                wrapper.addField(FieldSpec.builder(String.class, "TYPE_NAME")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("$T.class.getName()", wrappedClass)
                        .build());

                // Gen the init function as well as the __index and __namecall metamethods
                wrapper.addMethod(buildInitFunc(className, !properties.isEmpty(), !methods.isEmpty()));
                if (!properties.isEmpty()) wrapper.addMethod(buildIndexMetaMethod(wrappedClass, properties));
                if (!methods.isEmpty()) wrapper.addMethod(buildNameCallMetaMethod(wrappedClass, methods));

                JavaFile jf = JavaFile.builder(packageName, wrapper.build())
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build();

                try {
                    jf.writeTo(processingEnv.getFiler());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return true;
    }

    private @NotNull MethodSpec buildInitFunc(@NotNull String className, boolean hasIndex, boolean hasNameCall) {
        var method = MethodSpec.methodBuilder("initMetatable")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        method.addParameter(Types.LUA_STATE, "state");
        method.addStatement("state.newMetaTable(TYPE_NAME)");

        if (hasIndex) {
            method.addStatement("state.pushCFunction($N::$L, \"__index\")", className, "luaIndex");
            method.addStatement("state.setField(-2, \"__index\")");
        }
        if (hasNameCall) {
            method.addStatement("state.pushCFunction($N::$L, \"__namecall\")", className, "luaNameCall");
            method.addStatement("state.setField(-2, \"__namecall\")");
        }

        method.addStatement("state.pop(1)");
        return method.build();
    }

    private @NotNull MethodSpec buildIndexMetaMethod(@NotNull TypeName wrappedClass, @NotNull PropertyList properties) {
        var method = MethodSpec.methodBuilder("luaIndex")
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
        var method = MethodSpec.methodBuilder("luaNameCall")
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

    private @NotNull List<? extends Element> getAnnotatedMembers(@NotNull TypeElement elem, @NotNull TypeElement annotation) {
        return elementUtils.getAllMembers(elem).stream()
                .filter(e -> e.getAnnotationMirrors().stream()
                        .anyMatch(a -> a.getAnnotationType().asElement().equals(annotation)))
                .toList();
    }

    private void error(@NotNull Element elem, @NotNull String message) {
        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR, message, elem);
    }
}