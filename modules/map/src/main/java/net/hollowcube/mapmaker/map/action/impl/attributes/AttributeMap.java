package net.hollowcube.mapmaker.map.action.impl.attributes;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import net.minestom.server.codec.Codec;
import net.minestom.server.entity.attribute.Attribute;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class AttributeMap {

    public static final Codec<AttributeMap> CODEC = Attribute.CODEC
            .mapValue(Codec.DOUBLE)
            .transform(AttributeMap::new, it -> it.data);
    public static final AttributeMap EMPTY = new AttributeMap(Object2DoubleMaps.emptyMap());

    private final Object2DoubleArrayMap<Attribute> data = new Object2DoubleArrayMap<>();

    private AttributeMap(Map<? extends Attribute, ? extends Double> m) {
        this.data.putAll(m);
    }

    public AttributeMap() {
    }

    public AttributeMap with(Attribute attribute, double value) {
        AttributeMap copy = new AttributeMap(this.data);
        copy.data.put(attribute, value);
        return copy;
    }

    public double getOrDefault(final Attribute key, final double defaultValue) {
        return this.data.getOrDefault(key, defaultValue);
    }

    @Override
    public int hashCode() {
        return this.data.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AttributeMap other && this.data.equals(other.data);
    }

    @Override
    public String toString() {
        return String.format("AttributeMap%s", this.data);
    }
}
