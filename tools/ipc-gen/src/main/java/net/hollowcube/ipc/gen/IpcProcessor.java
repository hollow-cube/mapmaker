package net.hollowcube.ipc.gen;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Generates the two halves of an ipc service from the one interface that defines it.
///
/// For `@IpcClient interface FooClient`, in `FooClient`'s own package:
/// - `FooClientHttp implements FooClient` — every method becomes `POST /foo/<method-name>` with the
///   arguments as named fields of a JSON object and the return value as the whole response body.
///   The route drops a trailing `Client` from the interface name and kebab-cases both halves.
/// - `FooClientHandler implements HttpHandler` — the same routing read backwards, over any
///   implementation of `FooClient`.
///
/// Neither side names a route or a field as a string anyone writes: both are derived from the same
/// [ExecutableElement], so adding, renaming or retyping a method moves the client and the server
/// together or fails to compile.
@AutoService(Processor.class)
public final class IpcProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var annotation = processingEnv.getElementUtils().getTypeElement(IpcNames.IPC_ANNOTATION);
        if (annotation == null) return false;

        for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
            var model = model((TypeElement) element);
            if (model == null) continue;

            write(model.clientName(), ClientEmitter.emit(processingEnv.getMessager(), model));
            write(model.serverName(), ServerEmitter.emit(processingEnv.getMessager(), model));
        }
        return false;
    }

    /// Validates one annotated element, reporting every problem it finds rather than the first, and
    /// answering null if it found any.
    private @Nullable IpcModel model(TypeElement element) {
        var messager = processingEnv.getMessager();
        if (element.getKind() != ElementKind.INTERFACE) {
            messager.printError("@Ipc must be an interface", element);
            return null;
        }
        if (!element.getTypeParameters().isEmpty()) {
            messager.printError("@Ipc interfaces cannot be generic; there is nothing to bind "
                + "the type argument to on the wire", element);
            return null;
        }

        var ok = true;
        var methods = new ArrayList<IpcModel.Method>();
        var seen = new HashSet<String>();
        for (var method : abstractMethods(element)) {
            var name = method.getSimpleName().toString();
            if (!method.getTypeParameters().isEmpty()) {
                messager.printError("ipc methods cannot be generic", method);
                ok = false;
            }
            if (!seen.add(name)) {
                // The method name is the route, so an overload is two methods at one address.
                messager.printError("ipc methods cannot be overloaded: '" + name + "' is declared twice", method);
                ok = false;
            }
            methods.add(new IpcModel.Method(method, name, IpcNames.methodPath(name), method.getParameters(),
                method.getReturnType(), method.getReturnType().getKind() == javax.lang.model.type.TypeKind.VOID));
        }
        if (!ok) return null;

        var interfaceName = ClassName.get(element);
        var base = IpcNames.base(String.join("", interfaceName.simpleNames()));
        return new IpcModel(element, interfaceName,
            interfaceName.peerClass(base + IpcNames.CLIENT_SUFFIX),
            interfaceName.peerClass(base + IpcNames.SERVER_SUFFIX),
            IpcNames.servicePath(base), List.copyOf(methods));
    }

    /// Every method the generated client has to implement — including inherited ones, since leaving
    /// one out would produce a class that does not compile rather than a diagnostic.
    private List<ExecutableElement> abstractMethods(TypeElement element) {
        var object = processingEnv.getElementUtils().getTypeElement("java.lang.Object");
        var methods = new ArrayList<ExecutableElement>();
        for (var method : ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(element))) {
            if (!method.getModifiers().contains(Modifier.ABSTRACT)) continue;
            if (method.getEnclosingElement().equals(object)) continue;
            methods.add(method);
        }
        return methods;
    }

    private void write(ClassName name, @Nullable TypeSpec type) {
        if (type == null) return; // The emitter reported its own diagnostic.
        try {
            JavaFile.builder(name.packageName(), type)
                .skipJavaLangImports(true)
                .indent("    ")
                .build()
                .writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write " + name, e);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(IpcNames.IPC_ANNOTATION);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
