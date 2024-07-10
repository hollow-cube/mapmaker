package net.hollowcube.luau.ap;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.Map;

public interface TypeConverter {

    static @NotNull Map<TypeName, TypeConverter> collectTypeConverters(
            @NotNull RoundEnvironment roundEnv,
            @NotNull TypeElement luaTypeImpl
    ) {
        Map<TypeName, TypeConverter> typeConverters = new HashMap<>(SIMPLE_CONVERTER_MAP);
        for (var elem : roundEnv.getElementsAnnotatedWith(luaTypeImpl)) {
            var implType = TypeName.get(elem.asType());
            var anno = elem.getAnnotationMirrors().stream()
                    .filter(a -> a.getAnnotationType().asElement().equals(luaTypeImpl))
                    .findFirst().orElseThrow();
            var targetTypeMirror = anno.getElementValues().entrySet().stream()
                    .filter(e -> e.getKey().getSimpleName().contentEquals("type"))
                    .map(Map.Entry::getValue)
                    .map(v -> (TypeMirror) v.getValue())
                    .findFirst().orElseThrow();
            var targetType = TypeName.get(targetTypeMirror);
            typeConverters.put(targetType, new TypeConverter() {
                @Override
                public void insertPush(@NotNull MethodSpec.Builder method, @NotNull String getter) {
                    method.addStatement("$T.pushLuaValue(state, $L)", implType, getter);
                }

                @Override
                public void insertPop(@NotNull MethodSpec.Builder method, @NotNull String name, int index) {
                    method.addStatement("$T $L = $T.checkLuaArg(state, $L)", targetType, name, implType, index);
                }
            });
        }
        return typeConverters;
    }

    Map<TypeName, TypeConverter> SIMPLE_CONVERTER_MAP = Map.ofEntries(
            Map.entry(TypeName.get(String.class), new TypeConverter() {
                @Override
                public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
                    method.addStatement("state.pushString($L)", getter);
                }

                @Override
                public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
                    method.addStatement("String $L = state.checkStringArg($L)", name, index);
                }
            }),
            Map.entry(TypeName.BOOLEAN, new TypeConverter() {
                @Override
                public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
                    method.addStatement("state.pushBoolean($L)", getter);
                }

                @Override
                public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
                    method.addStatement("boolean $L = state.checkBooleanArg($L)", name, index);
                }
            })
    );

    @NotNull
    TypeConverter PIN = new TypeConverter() {
        @Override
        public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
            method.addStatement("(($T) $L).push(state)", Types.PIN_IMPL, getter);
        }

        @Override
        public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
            throw new UnsupportedOperationException("Pin cannot be popped");
        }
    };

    void insertPush(@NotNull MethodSpec.Builder method, @NotNull String getter);
    void insertPop(@NotNull MethodSpec.Builder method, @NotNull String name, int index);


}
