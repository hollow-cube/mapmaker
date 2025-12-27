package net.hollowcube.nativeimage;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeClassInitialization;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.graalvm.nativeimage.hosted.RuntimeResourceAccess;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

/// Responsible for doing a bunch of dynamic registration required for native image.
///
/// * Record classes in net.hollowcube.mapmaker are automatically registered for reflection (required for gson)
///   This is a bit too broad most likely, should look into just using RuntimeGson.
/// * @RuntimeGson classes are automatically registered for reflection.
/// * Canvas View implementations have their xml files registered as resources and actions/signals/etc are
///   registered for reflection
/// * Minestom MetadataDef subclasses are registered for runtime lookup.
public class HCNativeImageFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        var canvasClasses = new CanvasClasses(access);

        try (ScanResult scanResult = new ClassGraph()
            .overrideClasspath(access.getApplicationClassPath())
            .enableClassInfo()
            .enableFieldInfo()
            .enableAnnotationInfo()
            .acceptPackages("net.hollowcube.mapmaker", "net.hollowcube.posthog")
            .scan()) {

            scanResult.getSubclasses(Record.class)
                .forEach(ci -> processRecordClass(access, ci));

            scanResult.getClassesWithAnnotation("net.hollowcube.common.util.RuntimeGson")
                .forEach(ci -> processRuntimeGsonClass(access, ci));

            scanResult.getSubclasses("net.hollowcube.canvas.View")
                .forEach(classInfo -> processViewClass(access, canvasClasses, classInfo));

            processMinestomMetadataDef(access);
        }

        try (ScanResult scanResult = new ClassGraph()
            .overrideClasspath(access.getApplicationClassPath())
            .enableClassInfo()
            .acceptPackages("net.minestom", "net.kyori", "ch.qos.logback", "org.jctools", "it.unimi.dsi.fastutil", "com.google.gson")
            .scan()) {
            RuntimeClassInitialization.initializeAtBuildTime("net.hollowcube.mapmaker.isolate");
            for (var packageInfo : scanResult.getPackageInfo()) {
//                if ("net.minestom.server.network".equals(packageInfo.getName()))
//                    continue;
                if ("net.minestom.server.utils".equals(packageInfo.getName()))
                    continue;
                if ("net.minestom.server.network.packet".equals(packageInfo.getName()))
                    continue;
                System.out.println("PI: " + packageInfo.getName());
                RuntimeClassInitialization.initializeAtBuildTime(packageInfo.getName());
            }

            RuntimeClassInitialization.initializeAtRunTime(access.findClassByName("net.minestom.server.network.NetworkBufferImpl"));
            RuntimeClassInitialization.initializeAtRunTime(access.findClassByName("net.minestom.server.network.NetworkBufferImpl$CompressionHolder"));
            RuntimeClassInitialization.initializeAtRunTime(access.findClassByName("net.minestom.server.utils.PacketViewableUtils$ViewableStorage"));
            RuntimeClassInitialization.initializeAtRunTime(access.findClassByName("net.minestom.server.utils.ObjectPool"));
            RuntimeClassInitialization.initializeAtRunTime(access.findClassByName("net.minestom.server.network.player.PlayerSocketConnection"));
            RuntimeClassInitialization.initializeAtRunTime(access.findClassByName("net.minestom.server.network.packet.PacketVanilla"));
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
        for (var field : gsonClass.getDeclaredMethods())
            RuntimeReflection.register(field);
    }

    private void processViewClass(@NotNull BeforeAnalysisAccess access, @NotNull CanvasClasses classes, @NotNull ClassInfo ci) {
        var unnamed = access.getApplicationClassLoader().getUnnamedModule();
        var viewClass = access.findClassByName(ci.getName());

        // 1. Add the relevant xml file as an included resource
        var resourcePath = String.format("%s.xml", viewClass.getName().replace(".", "/"));
        RuntimeResourceAccess.addResource(unnamed, resourcePath);

        // 2. The class has to be accessible for Class.forName for imports
        RuntimeReflection.register(viewClass);

        // 3. The constructor which takes a Context has to be accessible for imports
        var contextConstructor = getContextConstructor(classes, viewClass);
        if (contextConstructor != null) RuntimeReflection.register(contextConstructor);

        // 4. All contextobject/outlet/outletgroup fields have to be writable
        RuntimeReflection.registerAllDeclaredFields(viewClass);
        RuntimeReflection.registerAllDeclaredMethods(viewClass);
        for (var field : viewClass.getDeclaredFields()) {
            boolean isRelevantField = field.getAnnotation(classes.outlet) != null
                                      || field.getAnnotation(classes.outletGroup) != null
                                      || field.getAnnotation(classes.contextObject) != null;
            if (isRelevantField) {
                RuntimeReflection.register(field);
            }
        }

        // 5. All action/signal methods have to be callable & discoverable
        for (var method : viewClass.getDeclaredMethods()) {
            boolean isRelevant = method.getAnnotation(classes.action) != null
                                 || method.getAnnotation(classes.signal) != null;
            if (isRelevant) {
                RuntimeReflection.register(method);
            }
        }
    }

    private Constructor<?> getContextConstructor(CanvasClasses classes, Class<?> viewClass) {
        try {
            return viewClass.getConstructor(classes.context);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private void processMinestomMetadataDef(BeforeAnalysisAccess access) {
        var metadataDefClass = access.findClassByName("net.minestom.server.entity.MetadataDef");
        RuntimeReflection.registerClassLookup(metadataDefClass.getName());

        // Also add all the subclasses.
        for (Class<?> subclass : metadataDefClass.getDeclaredClasses()) {
            RuntimeReflection.registerClassLookup(subclass.getName());
        }
    }

    private record CanvasClasses(
        @NotNull Class<?> context,
        @NotNull Class<? extends Annotation> contextObject,
        @NotNull Class<? extends Annotation> outlet,
        @NotNull Class<? extends Annotation> outletGroup,
        @NotNull Class<? extends Annotation> action,
        @NotNull Class<? extends Annotation> signal
    ) {
        @SuppressWarnings("unchecked")
        public CanvasClasses(BeforeAnalysisAccess access) {
            this(access.findClassByName("net.hollowcube.canvas.internal.Context"),
                (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.ContextObject"),
                (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.Outlet"),
                (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.OutletGroup"),
                (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.Action"),
                (Class<? extends Annotation>) access.findClassByName("net.hollowcube.canvas.annotation.Signal"));
        }
    }

}
