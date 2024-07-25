package net.hollowcube.luau.ap;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import net.hollowcube.luau.ap.proc.AbstractLuaProcessor;
import net.hollowcube.luau.ap.proc.LuaBindableProcessor;
import net.hollowcube.luau.ap.proc.LuaObjectProcessor;
import net.hollowcube.luau.ap.proc.LuaTypeImplProcessor;
import net.hollowcube.luau.ap.util.LuaTypeMirror;
import net.hollowcube.luau.ap.util.LuaTypeRegistry;
import net.hollowcube.luau.ap.util.ProcUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

@SupportedAnnotationTypes({"net.hollowcube.luau.annotation.LuaObject", "net.hollowcube.luau.annotation.LuaTypeImpl", "net.hollowcube.luau.annotation.LuaBindable"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class LuauAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;

    private TypeElement luaObject;
    private TypeElement luaTypeImpl;
    private TypeElement luaBindable;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.elementUtils = processingEnvironment.getElementUtils();

        this.luaObject = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaObject"), "LuaObject");
        this.luaTypeImpl = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaTypeImpl"), "LuaTypeImpl");
        this.luaBindable = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaBindable"), "LuaBindable");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var types = createTypeRegistry(roundEnv, luaTypeImpl, luaObject);

        {   // LuaObject
            AbstractLuaProcessor proc = new LuaObjectProcessor(processingEnv.getMessager(), elementUtils, types);
            processAnnotation(roundEnv, luaObject, proc);
        }

        {   // LuaTypeImpl
            AbstractLuaProcessor proc = new LuaTypeImplProcessor(processingEnv.getMessager(), elementUtils, types);
            processAnnotation(roundEnv, luaTypeImpl, proc);
        }

        {   // LuaBindable
            AbstractLuaProcessor proc = new LuaBindableProcessor(processingEnv.getMessager(), elementUtils, types);
            processAnnotation(roundEnv, luaBindable, proc);
        }

        return true;
    }

    private void processAnnotation(
            @NotNull RoundEnvironment roundEnv,
            @NotNull TypeElement annotation,
            @NotNull AbstractLuaProcessor proc
    ) {
        Messager log = processingEnv.getMessager();

        Set<? extends Element> annotatedElems = roundEnv.getElementsAnnotatedWith(annotation);
        for (Element elem : annotatedElems) {
            if (!(elem instanceof TypeElement typeElem)) {
                log.printError(annotation + " must be applied to a type", elem);
                continue;
            }

            try {
                final TypeSpec out = proc.process(typeElem);
                if (out == null) continue;

                TypeName wrappedClass = TypeName.get(typeElem.asType());
                if (wrappedClass instanceof ParameterizedTypeName ptn) wrappedClass = ptn.rawType;
                JavaFile jf = JavaFile.builder(((ClassName) wrappedClass).packageName(), out)
                        .skipJavaLangImports(true)
                        .indent("    ")
                        .build();
                jf.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                ProcUtil.logStackTrace(log, elem, e);
            }
        }
    }

    private @NotNull LuaTypeRegistry createTypeRegistry(
            @NotNull RoundEnvironment roundEnv,
            @NotNull TypeElement luaTypeImpl,
            @NotNull TypeElement luaObject
    ) {
        var registry = new LuaTypeRegistry();

        // Primitives (note that adding a boxed one will also register the unboxed one)
        registry.defineBasicPrimitive("boolean", TypeName.get(Boolean.class), "pushBoolean", "checkBooleanArg");
        registry.defineBasicPrimitive("number", TypeName.get(Double.class), "pushNumber", "checkNumberArg");
        registry.defineBasicPrimitive("number", TypeName.get(Integer.class), "pushInteger", "checkIntegerArg");
        registry.defineBasicPrimitive("string", TypeName.get(String.class), "pushString", "checkStringArg");

        // Find all the LuaTypeImpls
        for (var elem : roundEnv.getElementsAnnotatedWith(luaTypeImpl)) {
            var implType = TypeName.get(elem.asType());
            var typeImplAnnotation = Objects.requireNonNull(ProcUtil.getAnnotation(elem, Types.LUA_TYPE_IMPL));
            var targetTypeMirror = ProcUtil.getAnnotationValue(typeImplAnnotation, "type", TypeMirror.class);
            var targetLuaName = ProcUtil.getAnnotationValue(typeImplAnnotation, "name", String.class);

            var targetType = TypeName.get(targetTypeMirror);
            registry.define(targetType, new LuaTypeMirror() {
                @Override
                public @NotNull String luaType() {
                    return targetLuaName;
                }

                @Override
                public @NotNull TypeName javaType() {
                    return targetType;
                }

                @Override
                public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
                    method.addStatement("$T.pushLuaValue(state, $L)", implType, getter);
                }

                @Override
                public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
                    method.addStatement("$T $L = $T.checkLuaArg(state, $L)", targetType, name, implType, index);
                }

                @Override
                public @NotNull String createCheck(int index) {
                    return "false"; //todo
                }

                @Override
                public String toString() {
                    return luaType() + " -> " + javaType();
                }
            });
        }

        // Find all the LuaObjects
        for (var elem : roundEnv.getElementsAnnotatedWith(luaObject)) {
            var implType = TypeName.get(elem.asType());
            if (implType instanceof ParameterizedTypeName ptn) implType = ptn.rawType;
            var implClass = (ClassName) implType;
            var wrapperType = ClassName.get(implClass.packageName(), implClass.simpleName() + "$Wrapper");

            registry.define(implType, new LuaTypeMirror() {
                @Override
                public @NotNull String luaType() {
                    return implClass.simpleName().replace("Lua", "");
                }

                @Override
                public @NotNull TypeName javaType() {
                    return implClass;
                }

                @Override
                public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
                    //todo we need to add a push function to wrapper prob.
                    method.addStatement("state.newUserData($L)", getter);
                    method.addStatement("state.getMetaTable($T.TYPE_NAME)", wrapperType);
                    method.addStatement("state.setMetaTable(-2)");
                }

                @Override
                public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
                    method.addStatement("$T $L = ($T) $T.checkUserDataArg(state, $L, $T.class)", implClass, name, implClass, Types.LUA_HELPERS, index, implClass);
                }

                @Override
                public @NotNull String createCheck(int index) {
                    return "net.hollowcube.luau.util.LuaHelpers.isUserData(state, " + index + ", " + wrapperType.canonicalName() + ".TYPE_NAME)";
                }

                @Override
                public String toString() {
                    return luaType() + " -> " + javaType();
                }
            });
        }

        // Find all the LuaBindables
        for (var elem : roundEnv.getElementsAnnotatedWith(luaBindable)) {
            var implType = TypeName.get(elem.asType());
            if (implType instanceof ParameterizedTypeName ptn) implType = ptn.rawType;
            var implClass = (ClassName) implType;
            var wrapperType = ClassName.get(implClass.packageName(), implClass.simpleName() + "$Wrapper");

            registry.define(implType, new LuaTypeMirror() {
                @Override
                public @NotNull String luaType() {
                    return "function"; //todo actual type sig
                }

                @Override
                public @NotNull TypeName javaType() {
                    return implClass;
                }

                @Override
                public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
                    throw new UnsupportedOperationException("Not implemented");
                }

                @Override
                public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
                    //todo this is leaking global refs.
                    method.addStatement("$T $L = $T.bind($T.class, state, $L).get()", implClass, name, Types.LUA_FUNCTIONS, implClass, index);
                }

                @Override
                public @NotNull String createCheck(int index) {
                    return "state.isFunction(" + index + ")";
                }
            });
        }

        return registry;
    }
}