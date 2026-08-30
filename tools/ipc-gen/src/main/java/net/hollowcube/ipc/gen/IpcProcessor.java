package net.hollowcube.ipc.gen;

import com.google.auto.service.AutoService;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.TypeSpec;
import net.hollowcube.ipc.gen.wire.WireDescriptor.Use;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.*;

/// Generates everything the wire module needs from the types that define it.
///
/// For `@Ipc interface FooService`, in its own package:
/// - `FooClient implements FooService` — every method becomes `POST /foo/<method-name>` with the
///   arguments as named fields of a JSON object and the return value as the whole response body.
/// - `FooServer implements HttpHandler` — the same routing read backwards, over any implementation.
///
/// Then, for the wire as a whole — every method signature, every `@NatsMessage` and
/// `@NotificationBody` record, and every type reachable from one:
/// - `net.hollowcube.ipc.WireAdapters`, the gson adapters `Wire.gson()` carries, one per enum
///   and sealed interface, plus a `<Name>Unknown` record per sealed interface.
/// - `wire.json`, the descriptor `wireCheck` and `wireCompat` hold this build to.
///
/// Neither side names a route or a field as a string anyone writes: both are derived from the same
/// [ExecutableElement], so adding, renaming or retyping a method moves the client and the server
/// together or fails to compile. The wire descriptor is what catches the change that compiles fine
/// here but not against the client that is still running.
@AutoService(Processor.class)
public final class IpcProcessor extends AbstractProcessor {

    /// Where the descriptor lands in the class output, and so in the jar.
    static final String DESCRIPTOR_RESOURCE = "wire.json";

    private boolean emitted;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var elements = processingEnv.getElementUtils();
        var services = annotated(roundEnv, elements.getTypeElement(IpcNames.IPC_ANNOTATION));
        var messages = annotated(roundEnv, elements.getTypeElement(IpcNames.NATS_MESSAGE_ANNOTATION));
        var bodies = annotated(roundEnv, elements.getTypeElement(IpcNames.NOTIFICATION_BODY_ANNOTATION));
        if (services.isEmpty() && messages.isEmpty() && bodies.isEmpty()) return false;

        var messager = processingEnv.getMessager();
        if (emitted) {
            // Every hand-written root is in the first round; one that appears later was generated,
            // and the descriptor already written does not know it.
            for (var element : services) messager.printError("ipc: wire roots cannot be generated", element);
            return false;
        }
        emitted = true;

        var walker = new WireWalker(messager);
        var models = new ArrayList<IpcModel>();
        for (var element : services) {
            var model = model((TypeElement) element);
            if (model == null) continue;
            models.add(model);

            write(model.clientName(), ClientEmitter.emit(messager, model));
            write(model.serverName(), ServerEmitter.emit(messager, model));
            for (var method : model.methods()) {
                var call = element.getSimpleName() + "." + method.name();
                for (var parameter : method.parameters()) {
                    walker.root(parameter, parameter.asType(), Use.REQUEST,
                        call + "(" + parameter.getSimpleName() + ")");
                }
                if (!method.isVoid()) {
                    walker.root(method.element(), method.returnType(), Use.RESPONSE, call + "() returns");
                }
            }
        }
        models.sort(Comparator.comparing(IpcModel::path));

        var subjects = keyed(walker, messages, IpcNames.NATS_MESSAGE_ANNOTATION, "subject", Use.MESSAGE);
        var notifications = keyed(walker, bodies, IpcNames.NOTIFICATION_BODY_ANNOTATION, "type", Use.BODY);
        if (!walker.ok() || subjects == null || notifications == null) return false;

        write(IpcNames.WIRE_ADAPTERS, AdaptersEmitter.factory(walker));
        for (var sealed : walker.sealeds()) write(sealed.unknown(), AdaptersEmitter.unknownVariant(sealed));
        writeDescriptor(DescriptorBuilder.build(models, walker, subjects, notifications).toJson());
        return false;
    }

    private static List<? extends Element> annotated(RoundEnvironment roundEnv, @Nullable TypeElement annotation) {
        if (annotation == null) return List.of();
        var out = new ArrayList<>(roundEnv.getElementsAnnotatedWith(annotation));
        out.sort(Comparator.comparing(element -> element.asType().toString()));
        return out;
    }

    /// Walks the records marked with one keyed root annotation, answering key to record — or null
    /// having reported a problem.
    private @Nullable SortedMap<String, String> keyed(WireWalker walker, List<? extends Element> roots,
                                                      String annotation, String member, Use use) {
        var messager = processingEnv.getMessager();
        var out = new TreeMap<String, String>();
        var ok = true;
        for (var root : roots) {
            var element = (TypeElement) root;
            var key = annotationValue(element, annotation, member);
            var path = "@" + annotation.substring(annotation.lastIndexOf('.') + 1) + "(\"" + key + "\") " + element.getSimpleName();
            if (key.isBlank()) {
                messager.printError("ipc: " + member + " cannot be blank", element);
                ok = false;
            }
            var previous = out.put(key, element.getQualifiedName().toString());
            if (previous != null) {
                messager.printError("ipc: " + member + " '" + key + "' is claimed by both " + previous + " and "
                    + element.getQualifiedName(), element);
                ok = false;
            }
            if (!walker.rootRecord(element, use, path)) ok = false;
        }
        return ok ? out : null;
    }

    private static String annotationValue(TypeElement element, String annotation, String member) {
        for (var mirror : element.getAnnotationMirrors()) {
            var type = (TypeElement) mirror.getAnnotationType().asElement();
            if (!type.getQualifiedName().contentEquals(annotation)) continue;
            for (var entry : mirror.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals(member)) return String.valueOf(entry.getValue().getValue());
            }
        }
        return "";
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

    private void writeDescriptor(String json) {
        try {
            var file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", DESCRIPTOR_RESOURCE);
            try (Writer writer = file.openWriter()) {
                writer.write(json);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to write " + DESCRIPTOR_RESOURCE, e);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(IpcNames.IPC_ANNOTATION, IpcNames.NATS_MESSAGE_ANNOTATION, IpcNames.NOTIFICATION_BODY_ANNOTATION);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
