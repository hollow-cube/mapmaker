package net.hollowcube.nativeimage.helper;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

public class CanvasReflectionFeature implements Feature {
    private Class<?> contextClass;
    private Class<? extends Annotation> contextObjectClass;
    private Class<? extends Annotation> outletClass;
    private Class<? extends Annotation> outletGroupClass;
    private Class<? extends Annotation> actionClass;
    private Class<? extends Annotation> signalClass;

    @Override
    @SuppressWarnings("unchecked")
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        contextClass = access.findClassByName("net.hollowcube.canvas.internal.Context");
        contextObjectClass = (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.ContextObject");
        outletClass = (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.Outlet");
        outletGroupClass = (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.OutletGroup");
        actionClass = (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.Action");
        signalClass = (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.Signal");

        try (ScanResult scanResult = new ClassGraph()
                .overrideClasspath(access.getApplicationClassPath())
                .enableClassInfo()
                .enableFieldInfo()
                .enableAnnotationInfo()
                .acceptPackages("net.hollowcube.mapmaker")
                .scan()) {
            scanResult.getSubclasses("net.hollowcube.canvas.View")
                    .forEach(classInfo -> processViewClass(access, classInfo));
            scanResult.getSubclasses(Record.class)
                    .forEach(ci -> processRecordClass(access, ci));
            scanResult.getClassesWithAnnotation("net.hollowcube.common.util.RuntimeGson")
                    .forEach(ci -> processRuntimeGsonClass(access, ci));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processViewClass(BeforeAnalysisAccess access, ClassInfo info) {
        var unnamed = access.getApplicationClassLoader().getUnnamedModule();
        var viewClass = access.findClassByName(info.getName());

        // 1. Add the relevant xml file as an included resource
        var resourcePath = String.format("%s.xml", viewClass.getName().replace(".", "/"));
        RuntimeResourceAccess.addResource(unnamed, resourcePath);

        // 2. The class has to be accessible for Class.forName for imports
        RuntimeReflection.register(viewClass);

        // 3. The constructor which takes a Context has to be accessible for imports
        var contextConstructor = getContextConstructor(viewClass);
        if (contextConstructor != null) RuntimeReflection.register(contextConstructor);

        // 4. All contextobject/outlet/outletgroup fields have to be writable
        RuntimeReflection.registerAllDeclaredFields(viewClass);
        RuntimeReflection.registerAllDeclaredMethods(viewClass);
        for (var field : viewClass.getDeclaredFields()) {
            boolean isRelevantField = field.getAnnotation(outletClass) != null
                    || field.getAnnotation(outletGroupClass) != null
                    || field.getAnnotation(contextObjectClass) != null;
            if (isRelevantField) {
                RuntimeReflection.register(field);
            }
        }

        // 5. All action/signal methods have to be callable & discoverable
        for (var method : viewClass.getDeclaredMethods()) {
            boolean isRelevant = method.getAnnotation(actionClass) != null
                    || method.getAnnotation(signalClass) != null;
            if (isRelevant) {
                RuntimeReflection.register(method);
            }
        }
    }

    private void processRecordClass(BeforeAnalysisAccess access, ClassInfo info) {
        if (info.getName().endsWith("Event")) return;
        var recordClass = access.findClassByName(info.getName());

        RuntimeReflection.register(recordClass);
        for (var ctor : recordClass.getDeclaredConstructors())
            RuntimeReflection.register(ctor);
        for (var comp : recordClass.getRecordComponents())
            RuntimeReflection.register(comp.getAccessor());
    }

    private void processRuntimeGsonClass(BeforeAnalysisAccess access, ClassInfo info) {
        var gsonClass = access.findClassByName(info.getName());

        RuntimeReflection.register(gsonClass);
        for (var ctor : gsonClass.getDeclaredConstructors())
            RuntimeReflection.register(ctor);
        for (var field : gsonClass.getDeclaredFields())
            RuntimeReflection.register(field);

        System.out.println("Gson class: " + gsonClass.getName());
    }

    private Constructor<?> getContextConstructor(Class<?> viewClass) {
        try {
            return viewClass.getConstructor(contextClass);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

}
