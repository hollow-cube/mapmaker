package net.hollowcube.mapmaker.runtime.parkour.action.impl.velocity;

import net.hollowcube.molang.eval.MolangValue;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class MolangResolver<T> implements MolangValue.Holder {

    private final BiFunction<String, @Nullable T, @Nullable Double> resolver;
    private @Nullable T context = null;

    public MolangResolver(BiFunction<String, @Nullable T, @Nullable Double> resolver) {
        this.resolver = resolver;
    }

    public void setContext(@Nullable T context) {
        this.context = context;
    }

    @Override
    public MolangValue get(String field) {
        var value = this.resolver.apply(field, this.context);
        if (value == null) return MolangValue.NIL;
        return new MolangValue.Num(value);
    }
}
