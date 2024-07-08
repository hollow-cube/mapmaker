package net.hollowcube.luau.ap;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import net.hollowcube.luau.ap.proc.AbstractLuaProcessor;
import net.hollowcube.luau.ap.proc.LuaObjectProcessor;
import net.hollowcube.luau.ap.proc.LuaTypeImplProcessor;
import net.hollowcube.luau.ap.util.ProcUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SupportedAnnotationTypes({"net.hollowcube.luau.annotation.LuaObject", "net.hollowcube.luau.annotation.LuaTypeImpl"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class LuauAnnotationProcessor extends AbstractProcessor {

    private Elements elementUtils;

    private TypeElement luaObject;
    private TypeElement luaTypeImpl;

    private TypeElement luaProperty;
    private TypeElement luaMethod;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        this.elementUtils = processingEnvironment.getElementUtils();

        this.luaObject = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaObject"), "LuaObject");
        this.luaTypeImpl = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaTypeImpl"), "LuaTypeImpl");

        this.luaProperty = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaProperty"), "LuaProperty");
        this.luaMethod = Objects.requireNonNull(elementUtils.getTypeElement("net.hollowcube.luau.annotation.LuaMethod"), "LuaMethod");

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
                log.printError(annotation + " must be applied to a class", elem);
                continue;
            }
            if (!"java.lang.Object".equals(typeElem.getSuperclass().toString())) {
                error(elem, annotation + " must not have a superclass");
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

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Find available external type converters to include in processing.
        var typeConverters = TypeConverter.collectTypeConverters(roundEnv, luaTypeImpl);
        //todo i probably need to add the associated type impl to the elements which create an output.

        {   // LuaObject
            LuaObjectProcessor proc = new LuaObjectProcessor(processingEnv.getMessager(), elementUtils, typeConverters);
            processAnnotation(roundEnv, luaObject, proc);
        }

        {   // LuaTypeImpl
            LuaTypeImplProcessor proc = new LuaTypeImplProcessor(processingEnv.getMessager(), elementUtils, typeConverters);
            processAnnotation(roundEnv, luaTypeImpl, proc);
        }

//        for (TypeElement annotation : annotations) {
//            Set<? extends Element> annotatedElems = roundEnv.getElementsAnnotatedWith(annotation);
//
//            for (Element elem : annotatedElems) {
//                var typeElem = (TypeElement) elem;
//                if (!"java.lang.Object".equals(typeElem.getSuperclass().toString())) {
//                    error(elem, "LuaObject must not have a superclass");
//                    continue;
//                }
//
//                var packageName = elementUtils.getPackageOf(typeElem).getQualifiedName().toString();
//                var wrappedClass = TypeName.get(typeElem.asType());
//                if (wrappedClass instanceof ParameterizedTypeName ptn) {
//                    wrappedClass = ptn.rawType;
//                }
//                var className = typeElem.getSimpleName().toString() + "$Wrapper";
//
//                TypeSpec.Builder wrapper = TypeSpec.classBuilder(className)
//                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
//

//
//                // Gen a constant for the metatable name
//                wrapper.addField(FieldSpec.builder(String.class, "TYPE_NAME")
//                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
//                        .initializer("$T.class.getName()", wrappedClass)
//                        .build());
//
//                // Gen the init function as well as the __index and __namecall metamethods
//                wrapper.addMethod(buildInitFunc(className, !properties.isEmpty(), !methods.isEmpty()));
//                if (!properties.isEmpty()) wrapper.addMethod(buildIndexMetaMethod(wrappedClass, properties));
//                if (!methods.isEmpty()) wrapper.addMethod(buildNameCallMetaMethod(wrappedClass, methods));
//
//                JavaFile jf = JavaFile.builder(packageName, wrapper.build())
//                        .skipJavaLangImports(true)
//                        .indent("    ")
//                        .build();
//
//                try {
//                    jf.writeTo(processingEnv.getFiler());
//                } catch (IOException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }

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