package net.hollowcube.tools.compile;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.ClassFinder;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.comp.Modules;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

// MUST COMPILE WITH JAVA 11
// This is a Bazel limitation in building `java_plugin` dependencies as far as I can tell.
public class BlockingClassRewriter extends TreeScanner<Void, Void> {
    private final Context context;
    private final Log log;

    private final Names symbolTable;
    private final Name futureUtilClassName;
    private final Name assertThreadMethodName;

    private final Symbol.ModuleSymbol defaultModule;
    private final Symbol.ClassSymbol futureUtilClass;

    public BlockingClassRewriter(Context context, Log log) {
        this.context = context;
        this.log = log;

        symbolTable = Names.instance(context);
        futureUtilClassName = symbolTable.fromString("net.hollowcube.common.util.FutureUtil");
        assertThreadMethodName = symbolTable.fromString("assertThread");

        defaultModule = Modules.instance(context).getDefaultModule();
        futureUtilClass = ClassFinder.instance(context).loadClass(defaultModule, futureUtilClassName);
    }

    @Override
    public Void visitMethod(MethodTree node, Void unused) {
        JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) node;

        // Things to never modify
        boolean visitable = !methodDecl.name.toString().equals("<init>") && // Constructors
                (methodDecl.mods.flags & Flags.SYNTHETIC) == 0; // Synthetic methods
        if (!visitable) return super.visitMethod(node, unused);

        TreeMaker factory = TreeMaker.instance(context);
        factory.at(methodDecl.body.pos);

        // Get an identifier referencing the FutureUtil class
        JCTree.JCIdent futureUtilIdent = factory.Ident(futureUtilClassName);
        futureUtilIdent.sym = futureUtilClass;
        futureUtilIdent.type = futureUtilIdent.sym.type;

        // Access the assertThread method on the FutureUtil class
        JCTree.JCFieldAccess assertThreadField = factory.Select(futureUtilIdent, assertThreadMethodName);
        assertThreadField.sym = futureUtilClass.members().findFirst(assertThreadMethodName);
        assertThreadField.type = assertThreadField.sym.type;

        // Create the no args static method call
        JCTree.JCMethodInvocation call = factory.Apply(List.nil(), assertThreadField, List.nil());
        call.type = call.meth.type.getReturnType();

        // Finally prepend the method call to the function body (wrapped as a statement, not expr)
        methodDecl.body.stats = methodDecl.body.stats.prepend(factory.Exec(call));

        return super.visitMethod(node, unused);
    }
}
