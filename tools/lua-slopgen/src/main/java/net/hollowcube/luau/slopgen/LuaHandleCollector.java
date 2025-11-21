package net.hollowcube.luau.slopgen;

import com.palantir.javapoet.TypeName;
import com.sun.source.util.DocTrees;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.old.LuaMeta;
import net.hollowcube.luau.annotation.old.LuaStatic;
import net.hollowcube.luau.gen.LuaProperty;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.SimpleElementVisitor14;
import java.util.List;

public class LuaHandleCollector extends SimpleElementVisitor14<Void, List<LuaHandle>> {
    private final Messager messager;
    private final DocTrees docTrees;

    public LuaHandleCollector(Messager messager, DocTrees docTrees) {
        this.messager = messager;
        this.docTrees = docTrees;
    }

    @Override
    public Void visitType(TypeElement e, List<LuaHandle> luaHandles) {
        // Visit all enclosed elements (methods, fields, constructors, etc.)
        for (Element enclosedElement : e.getEnclosedElements()) {
            enclosedElement.accept(this, luaHandles);
        }
        return super.visitType(e, luaHandles);
    }

    @Override
    public Void visitExecutable(ExecutableElement e, List<LuaHandle> luaHandles) {
        var isPublic = e.getModifiers().stream().anyMatch(m -> m == Modifier.PUBLIC);
        if (!isPublic) return null;

        if (e.getParameters().size() != 1 || !e.getParameters().getFirst().asType().toString().equals(LuaState.class.getName()))
            return null;
        if (e.getReturnType().getKind() != TypeKind.INT)
            return null;

        var luaMeta = e.getAnnotation(LuaMeta.class);

        messager.printWarning("docs: " + docTrees.getDocCommentTree(e), e);

        luaHandles.add(new LuaHandle(
            TypeName.get(e.getEnclosingElement().asType()),
            e.getSimpleName().toString(),
            e.getAnnotation(LuaStatic.class) != null,
            e.getModifiers().stream().anyMatch(m -> m == Modifier.STATIC),
            e.getAnnotation(LuaProperty.class) != null,
            luaMeta != null ? luaMeta.value() : null
        ));

        return super.visitExecutable(e, luaHandles);
    }

}
