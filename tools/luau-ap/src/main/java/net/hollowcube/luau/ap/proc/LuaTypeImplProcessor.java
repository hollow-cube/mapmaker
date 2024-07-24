package net.hollowcube.luau.ap.proc;

import com.squareup.javapoet.*;
import net.hollowcube.luau.ap.Methods;
import net.hollowcube.luau.ap.Types;
import net.hollowcube.luau.ap.util.LuaTypeRegistry;
import net.hollowcube.luau.ap.util.ProcUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LuaTypeImplProcessor extends AbstractLuaProcessor {

    public LuaTypeImplProcessor(@NotNull Messager log, @NotNull Elements elements, @NotNull LuaTypeRegistry types) {
        super(log, elements, types);
    }

    @Override
    public @Nullable TypeSpec process(@NotNull TypeElement typeElem) {
        var packageName = elements.getPackageOf(typeElem).getQualifiedName().toString();
        var wrappedClass = TypeName.get(typeElem.asType());
        if (wrappedClass instanceof ParameterizedTypeName ptn) {
            wrappedClass = ptn.rawType;
        }
        var className = typeElem.getSimpleName().toString() + "$Wrapper";
        var wrapperClass = ClassName.get(packageName, className);

        // Find the annotation and target type (the type being implemented for)
        AnnotationMirror luaTypeImpl = ProcUtil.getAnnotation(typeElem, Types.LUA_TYPE_IMPL);
        if (luaTypeImpl == null) {
            log.printError("LuaTypeImpl annotation not found", typeElem);
            return null;
        }
        TypeName targetType = TypeName.get(ProcUtil.getAnnotationValue(luaTypeImpl, "type", TypeMirror.class));

        // Ensure that the type contains the required type conversion methods.
        if (!findRequiredConverters(typeElem, targetType)) return null;

        // Locate any meta methods to add to the meta table
        var metaMethods = Methods.collect(log, elements, types, targetType,
                ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_META));

        // Ensure no properties or methods are declared
        for (Element member : ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_PROPERTY))
            log.printError("@LuaProperty is not allowed in a LuaTypeImpl", member);
        for (Element member : ProcUtil.getAnnotatedMembers(elements, typeElem, Types.LUA_METHOD))
            log.printError("@LuaMethod is not allowed in a LuaTypeImpl", member);

        // Build the wrapper class
        TypeSpec.Builder wrapper = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Gen a constant for the metatable name
        wrapper.addField(FieldSpec.builder(String.class, "TYPE_NAME")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .initializer("$T.class.getName()", wrappedClass)
                .build());

        // Generate the metatable init method.
        appendInitFunc(wrapper, wrappedClass, wrapperClass, targetType, wrappedClass, metaMethods);

        return wrapper.build();
    }

    private boolean findRequiredConverters(@NotNull TypeElement typeElem, @NotNull TypeName targetType) {
        List<String> expected = List.of("pushLuaValue", "checkLuaArg");
        List<String> remaining = new ArrayList<>(expected);
        for (Element member : elements.getAllMembers(typeElem)) {
            if (!(member instanceof ExecutableElement method)) continue;
            final String methodName = method.getSimpleName().toString();
            if (!expected.contains(methodName)) continue;
            final String descriptor = typeElem + "." + methodName;

            // Method is one of the required ones.

            final Set<Modifier> mods = method.getModifiers(); // These functions must be public and static
            if (!mods.contains(Modifier.PUBLIC) || !mods.contains(Modifier.STATIC)) {
                log.printError(descriptor + " must be public and static", method);
                continue;
            }

            // Method must have the correct signature
            List<TypeName> paramTypes = new ArrayList<>();
            for (VariableElement param : method.getParameters())
                paramTypes.add(TypeName.get(param.asType()));
            TypeName returnType = TypeName.get(method.getReturnType());

            if ("pushLuaValue".equals(methodName)) {
                boolean valid = returnType.equals(TypeName.VOID)
                        && paramTypes.size() == 2
                        && paramTypes.get(0).equals(Types.LUA_STATE)
                        && paramTypes.get(1).equals(targetType);
                if (!valid) {
                    log.printError("Signature must be exactly void" + descriptor + "(LuaState, " + targetType + ")");
                    continue;
                }
                remaining.remove(methodName);
                continue;
            }

            if ("checkLuaArg".equals(methodName)) {
                boolean valid = returnType.equals(targetType)
                        && paramTypes.size() == 2
                        && paramTypes.get(0).equals(Types.LUA_STATE)
                        && paramTypes.get(1).equals(TypeName.INT);
                if (!valid) {
                    log.printError("Signature must be exactly " + targetType + " " + descriptor + "(LuaState, int)");
                    continue;
                }
                remaining.remove(methodName);
                continue;
            }

            log.printError("unreachable", method);
        }

        if (!remaining.isEmpty()) {
            log.printError("Missing required methods: " + remaining, typeElem);
            return false;
        }
        return true;
    }
}
