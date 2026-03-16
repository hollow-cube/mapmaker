package net.hollowcube.mapmaker.object;

import net.hollowcube.mapmaker.map.MapVariant;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

record ObjectTypeImpl(
    Key key,
    int cost,
    @Nullable MapVariant requiredVariant,
    @Nullable String requiredSubVariant
) implements ObjectType {

    static final Map<String, ObjectType> REGISTRY = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return key.asString();
    }

}
