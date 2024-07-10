package net.hollowcube.luau.ap;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;
import net.hollowcube.luau.ap.proc.AbstractLuaProcessor;
import net.hollowcube.luau.ap.proc.LuaBindableProcessor;
import net.hollowcube.luau.ap.proc.LuaObjectProcessor;
import net.hollowcube.luau.ap.proc.LuaTypeImplProcessor;
import net.hollowcube.luau.ap.util.ProcUtil;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
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
        // Find available external type converters to include in processing.
        var typeConverters = TypeConverter.collectTypeConverters(roundEnv, luaTypeImpl);
        //todo i probably need to add the associated type impl to the elements which create an output.

        {   // LuaObject
            AbstractLuaProcessor proc = new LuaObjectProcessor(processingEnv.getMessager(), elementUtils, typeConverters);
            processAnnotation(roundEnv, luaObject, proc);
        }

        {   // LuaTypeImpl
            AbstractLuaProcessor proc = new LuaTypeImplProcessor(processingEnv.getMessager(), elementUtils, typeConverters);
            processAnnotation(roundEnv, luaTypeImpl, proc);
        }

        {   // LuaBindable
            AbstractLuaProcessor proc = new LuaBindableProcessor(processingEnv.getMessager(), elementUtils, typeConverters);
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
            final TypeMirror superClass = typeElem.getSuperclass();
            if (!(superClass.getKind() == TypeKind.NONE || "java.lang.Object".equals(superClass.toString()))) {
                log.printError(annotation + " must not have a superclass " + typeElem.getSuperclass(), elem);
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
}