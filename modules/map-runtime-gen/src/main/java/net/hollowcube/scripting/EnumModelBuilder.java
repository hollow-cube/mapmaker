package net.hollowcube.scripting;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import net.hollowcube.scripting.docs.JavadocTagParser;
import net.hollowcube.scripting.gen.LuaEnum;
import net.hollowcube.scripting.gen.LuaLibrary;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

/// Builds a [Model.EnumDecl] from a `@LuaEnum`-annotated [TypeElement]. Shared by
/// [LibraryModelBuilder] (inner enums) and [LuaApiProcessor] (top-level enums) so the validation
/// and shape of the resulting `EnumDecl` is identical regardless of position.
///
/// Returns null and prints a diagnostic when the annotated type isn't a Java enum — the rest of
/// the build proceeds without this enum so the author sees as much of the build output as
/// possible in one shot.
public final class EnumModelBuilder {

    private EnumModelBuilder() {
    }

    /// `glueType` is the class where the push/check methods will be appended:
    ///
    ///  - inner enum of a library → enclosing `<Lib>$luau`
    ///  - top-level enum → its own `<Enum>$luau`
    public static @Nullable Model.EnumDecl build(
        ProcessingEnvironment env, Idents idents,
        TypeElement enumElement, ClassName glueType, LuaLibrary.Scope scope
    ) {
        if (enumElement.getKind() != ElementKind.ENUM) {
            env.getMessager().printError(
                "@LuaEnum can only be applied to a Java enum class", enumElement);
            return null;
        }

        var ann = enumElement.getAnnotation(LuaEnum.class);
        String luaName = (ann != null && !ann.name().isEmpty())
            ? ann.name()
            : enumElement.getSimpleName().toString();

        var sourceType = (ClassName) TypeName.get(enumElement.asType());
        int tag = idents.allocLightUserDataTag();

        var constants = new ArrayList<Model.EnumConstant>();
        int ordinal = 0;
        for (var member : enumElement.getEnclosedElements()) {
            if (member.getKind() != ElementKind.ENUM_CONSTANT) continue;
            var javaName = member.getSimpleName().toString();
            var pascalName = LuaNames.snakeToPascal(javaName);
            var docs = JavadocTagParser.parse(env.getElementUtils().getDocComment(member));
            constants.add(new Model.EnumConstant(javaName, pascalName, ordinal++, docs.description()));
        }

        var classDocs = JavadocTagParser.parse(env.getElementUtils().getDocComment(enumElement));
        return new Model.EnumDecl(sourceType, glueType, luaName, scope, tag,
            List.copyOf(constants), classDocs.description());
    }
}
