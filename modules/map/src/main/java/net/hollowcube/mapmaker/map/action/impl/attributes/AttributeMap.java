package net.hollowcube.mapmaker.map.action.impl.attributes;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMaps;
import net.minestom.server.codec.Codec;
import net.minestom.server.entity.attribute.Attribute;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class AttributeMap extends Object2DoubleArrayMap<Attribute> implements Map<Attribute, Double> {

    public static final Codec<AttributeMap> CODEC = Attribute.CODEC.mapValue(Codec.DOUBLE).transform(AttributeMap::new, it -> it);
    public static final AttributeMap EMPTY = new AttributeMap(Object2DoubleMaps.emptyMap());

    private AttributeMap(Map<? extends Attribute, ? extends Double> m) {
        super(m);
    }

    public AttributeMap() {
        super();
    }


}
