package net.hollowcube.mapmaker.runtime.parkour.action.impl.variables;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import net.minestom.server.codec.Codec;
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

    public Map<String, Double> view() {
        return Object2DoubleMaps.unmodifiable(this.variables);
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
}
