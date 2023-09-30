package net.hollowcube.tools.compile;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.*;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Log;
import org.burningwave.core.assembler.StaticComponentContainer;
import org.burningwave.core.function.ThrowingRunnable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;

// MUST COMPILE WITH JAVA 11
// This is a Bazel limitation in building `java_plugin` dependencies as far as I can tell.
public class CompilePlugin implements Plugin {

    static {
        try {
            StaticComponentContainer.Modules.exportToAllUnnamed("jdk.compiler");
            // force classes to be loaded: https://github.com/burningwave/core/discussions/15
            ThrowingRunnable.class.getClassLoader();
        } catch (Throwable ignored) {
            // do nothing
        }
    }

    @Override
    public String getName() {
        return "HollowCubeCompilePlugin";
    }

    @Override
    public void init(JavacTask t, String... args) {
        // Do not run when compiling the "stripped" binary
        if (System.getenv("HOLLOWCUBE_STRIPPED_BINARY") != null) return;

        if (t instanceof BasicJavacTask) {
            BasicJavacTask task = (BasicJavacTask) t;
            Log log = Log.instance(task.getContext());
            task.addTaskListener(new PluginTaskListener(task, log));
            log.printRawLines(Log.WriterKind.NOTICE, "hello from hollow cube compile plugin");
        }
    }

    public static final class PluginTaskListener implements TaskListener {
        private final BasicJavacTask task;
        private final Log log;

        public PluginTaskListener(BasicJavacTask task, Log log) {
            this.task = task;
            this.log = log;
        }

        @Override
        public void started(TaskEvent e) {
        }

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                e.getCompilationUnit().accept(new TreeScanner<Void, Void>() {
                    @Override
                    public Void visitClass(ClassTree node, Void unused) {
                        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) node;
                        if (classDecl.sym.isInterface() || !isBlockingClass(classDecl))
                            return super.visitClass(node, unused);

                        log.printRawLines(Log.WriterKind.ERROR, "Blocking class: " + classDecl.sym.name);
                        classDecl.accept(new BlockingClassRewriter(task.getContext(), log), null);

                        return super.visitClass(node, unused);
                    }
                }, null);
            }
        }

        private boolean isBlockingClass(JCTree.JCClassDecl classDecl) {

            // If the class itself is annotated with @Blocking, its blocking.
            for (Attribute.Compound annotation : classDecl.sym.getAnnotationMirrors()) {
                if (isBlockingAnnotation(annotation)) {
                    return true;
                }
            }

            // If the superclass is annotated with @Blocking, its blocking.
            Type superclass = classDecl.sym.getSuperclass();
            while (superclass != null && superclass.getKind() != TypeKind.NONE) {
                for (Attribute.Compound annotation : superclass.tsym.getAnnotationMirrors()) {
                    if (!isBlockingAnnotation(annotation)) continue;
                    return true;
                }

                //todo how to get superclass of tsym
                superclass = null;
//                superclass = superclass.tsym..getSuperclass();
            }

            // If any interface is annotated with @Blocking, its blocking.
            for (Type superInt : classDecl.sym.getInterfaces()) {
                for (Attribute.Compound annotation : superInt.tsym.getAnnotationMirrors()) {
                    if (!isBlockingAnnotation(annotation)) continue;
                    return true;
                }
            }

            return false;
        }

        private boolean isBlockingAnnotation(AnnotationMirror annotation) {
            return annotation.getAnnotationType().toString().equals("org.jetbrains.annotations.Blocking");
        }
    }

}
