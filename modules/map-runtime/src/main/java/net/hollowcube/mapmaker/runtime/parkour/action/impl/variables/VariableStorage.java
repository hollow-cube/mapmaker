package net.hollowcube.mapmaker.runtime.parkour.action.impl.variables;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.hollowcube.molang.eval.MolangValue;
import net.minestom.server.codec.Codec;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class VariableStorage {

    public static final Codec<VariableStorage> CODEC = Codec.STRING
            .mapValue(Codec.DOUBLE)
            .transform(VariableStorage::new, it -> it.variables);
    private final Object2DoubleMap<String> variables = new Object2DoubleOpenHashMap<>();

    public VariableStorage() {
    }

    private VariableStorage(Map<? extends String, ? extends Double> variables) {
        this.variables.putAll(variables);
    }

    public VariableStorage with(String name, double value) {
        VariableStorage copy = new VariableStorage(this.variables);
        copy.variables.put(name, value);
        return copy;
    }

    public double getOrDefault(String name, double defaultValue) {
        return this.variables.getOrDefault(name, defaultValue);
    }

    public void set(String name, double value) {
        this.variables.put(name, value);
    }

    @Override
    public int hashCode() {
        return this.variables.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VariableStorage other && this.variables.equals(other.variables);
    }

    @Override
    public String toString() {
        return String.format("VariableStorage%s", this.variables);
    }

    public static VariableStorage.MolangLookup lookup() {
        return new MolangLookup();
    }

    public static class MolangLookup implements MolangValue.Holder {

        private @Nullable VariableStorage storage = null;

        public void setStorage(@Nullable VariableStorage storage) {
            this.storage = storage;
        }

        @Override
        public MolangValue get(String field) {
            return new MolangValue.Num(this.storage != null ? this.storage.getOrDefault(field, 0.0) : 0.0);
        }
    }
}
