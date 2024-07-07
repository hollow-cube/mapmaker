package net.hollowcube.luau.ap;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface TypeConverter {

    Map<TypeName, TypeConverter> CONVERTER_MAP = Map.ofEntries(
            Map.entry(TypeName.get(String.class), new TypeConverter() {
                @Override
                public void insertPush(MethodSpec.@NotNull Builder method, @NotNull String getter) {
                    method.addStatement("state.pushString($L)", getter);
                }

                @Override
                public void insertPop(MethodSpec.@NotNull Builder method, @NotNull String name, int index) {
                    method.addStatement("String $L = state.checkStringArg($L)", name, index);
                }
            })
    );

    void insertPush(@NotNull MethodSpec.Builder method, @NotNull String getter);
    void insertPop(@NotNull MethodSpec.Builder method, @NotNull String name, int index);


}
