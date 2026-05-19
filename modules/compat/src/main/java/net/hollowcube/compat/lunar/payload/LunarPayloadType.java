package net.hollowcube.compat.lunar.payload;

import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNullByDefault;

import java.util.Map;

@NotNullByDefault
public record LunarPayloadType<T extends LunarPayload>(String name, StructCodec<T> codec) {

    private static final String QUALIFIER = "type.googleapis.com/";

    public static final Map<String, LunarPayloadType<?>> REGISTRY = Map.of(
        InstalledModsResponsePagePayload.TYPE.fullyQualifiedName(), InstalledModsResponsePagePayload.TYPE
    );

    public String fullyQualifiedName() {
        return QUALIFIER + this.name;
    }
}
