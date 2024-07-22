package net.hollowcube.luau.ap.proc;

import com.squareup.javapoet.*;
import net.hollowcube.luau.ap.MethodList;
import net.hollowcube.luau.ap.TypeConverter;
import net.hollowcube.luau.ap.Types;
import net.hollowcube.luau.ap.util.LuaTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LuaBindableProcessor extends AbstractLuaProcessor {

    public LuaBindableProcessor(@NotNull Messager log, @NotNull Elements elements, @NotNull Map<TypeName, TypeConverter> typeConverters, @NotNull LuaTypeRegistry types) {
        super(log, elements, typeConverters, types);
    }

    @Override
    public @Nullable TypeSpec process(@NotNull TypeElement typeElem) {
        var packageName = elements.getPackageOf(typeElem).getQualifiedName().toString();
        var wrappedClass = TypeName.get(typeElem.asType());
        if (wrappedClass instanceof ParameterizedTypeName) {
            log.printError("LuaBindable interfaces may not have type parameters", typeElem);
            return null;
        }
        var className = typeElem.getSimpleName().toString() + "$Binder";
        var wrapperClass = ClassName.get(packageName, className);

        // Find and process the relevant method on the interface
        if (!typeElem.getKind().isInterface()) {
            log.printError("LuaBindable must be applied to an interface", typeElem);
            return null;
        }
        List<ExecutableElement> possibleMethods = new ArrayList<>();
        for (Element member : elements.getAllMembers(typeElem)) {
            if (!(member instanceof ExecutableElement method)) continue;
            final Set<Modifier> mods = method.getModifiers();
            if (mods.contains(Modifier.STATIC) || mods.contains(Modifier.DEFAULT)) continue;
            if (!mods.contains(Modifier.ABSTRACT)) continue;

            possibleMethods.add(method);
        }
        if (possibleMethods.size() != 1) {
            log.printError("LuaBindable must have exactly one abstract method", typeElem);
            return null;
        }

        MethodList.Method meth = MethodList.single(log, typeConverters, possibleMethods.get(0), "bind", "bind");
        if (meth == null) return null;
        if (meth.ret() != null) {
            log.printError("LuaBindable method must not return a value", typeElem);
            return null; //todo
        }

        // Build the binder class
        TypeSpec.Builder binder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        // Implement the binder interface & auto service for it.
        binder.addSuperinterface(Types.LUA_BINDER);
        binder.addAnnotation(AnnotationSpec.builder(Types.AUTO_SERVICE)
                .addMember("value", "$T.class", Types.LUA_BINDER)
                .build());

        binder.addMethod(MethodSpec.methodBuilder("target")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(Class.class))
                .addStatement("return $T.class", wrappedClass)
                .build());

        binder.addMethod(buildBindMethod(wrappedClass, meth));

        return binder.build();
    }

    private @NotNull MethodSpec buildBindMethod(@NotNull TypeName wrappedClass, @NotNull MethodList.Method meth) {
        var method = MethodSpec.methodBuilder("bind")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Types.LUA_STATE, "state")
                .addParameter(int.class, "impl$index")
                .returns(ParameterizedTypeName.get(Types.PIN, wrappedClass));

        method.addStatement("state.checkType($N, $T.FUNCTION)", "impl$index", Types.LUA_TYPE);
        method.addStatement("final int $N = state.ref($N)", "l$ref", "impl$index");

        String argString = meth.args().stream()
                .map(MethodList.Arg::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        // Build the lambda impl
        method.addCode("return $T.fromRef(state, $N, ($L) -> {$>\n", Types.PIN, "l$ref", argString);

        //todo modify arg names to include some $ instead of the locals we declare here. probably just use arg indexes like arg0, arg1
        method.addStatement("state.getref($N)", "l$ref");
        for (var arg : meth.args()) {
            arg.type().insertPush(method, arg.name());
        }
        method.addStatement("state.pcall($L, $L)", meth.args().size(), meth.ret() == null ? 0 : 1);
        //todo returns

        method.addCode("$<});");
        return method.build();
    }
}
