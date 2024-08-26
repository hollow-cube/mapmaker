package net.hollowcube.aj.util;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ExtraCodecs {

    public static final Codec<UUID> UUID_STRING = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return DataResult.error(e.getMessage());
        }
    }, UUID::toString);

    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
    public static final Codec<Component> TEXT_COMPONENT = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                try {
                    var json = dynamic.convert(JsonOps.INSTANCE).getValue();
                    return DataResult.success(GSON_COMPONENT_SERIALIZER.deserializeFromTree(json));
                } catch (Exception e) {
                    return DataResult.error(e.getMessage());
                }
            },
            component -> new Dynamic<>(JsonOps.INSTANCE, GSON_COMPONENT_SERIALIZER.serializeToTree(component))
    );

    public static final Codec<JsonObject> JSON_OBJECT = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                try {
                    var json = dynamic.convert(JsonOps.INSTANCE).getValue();
                    if (!json.isJsonObject()) return DataResult.error("Expected a JSON object");
                    return DataResult.success(json.getAsJsonObject());
                } catch (Exception e) {
                    return DataResult.error(e.getMessage());
                }
            },
            object -> new Dynamic<>(JsonOps.INSTANCE, object)
    );

    public static final Codec<Material> MATERIAL = Codec.STRING.comapFlatMap(s -> {
        var material = Material.fromNamespaceId(s);
        if (material == null) return DataResult.error("Unknown item: " + s);
        return DataResult.success(material);
    }, Material::name);

    public static final Codec<Block> BLOCK = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(ArgumentBlockState.staticParse(s));
        } catch (ArgumentSyntaxException e) {
            return DataResult.error(e.getMessage());
        }
    }, Block::name);

    public static final Codec<float[]> FLOAT_2 = Codec.FLOAT.listOf().comapFlatMap(list -> {
        if (list.size() != 2) return DataResult.error("Expected 2 elements");
        return DataResult.success(new float[]{list.get(0), list.get(1)});
    }, list -> List.of(list[0], list[1]));

    public static final Codec<float[]> FLOAT_3 = Codec.FLOAT.listOf().comapFlatMap(list -> {
        if (list.size() != 3) return DataResult.error("Expected 3 elements");
        return DataResult.success(new float[]{list.get(0), list.get(1), list.get(2)});
    }, list -> List.of(list[0], list[1], list[2]));

    public static final Codec<float[]> FLOAT_4 = Codec.FLOAT.listOf().comapFlatMap(list -> {
        if (list.size() != 4) return DataResult.error("Expected 4 elements");
        return DataResult.success(new float[]{list.get(0), list.get(1), list.get(2), list.get(3)});
    }, list -> List.of(list[0], list[1], list[2], list[3]));

    public static <E extends Enum<E>> Codec<E> enumString(@NotNull Class<E> enumType) {
        return Codec.STRING.comapFlatMap(s -> {
            try {
                return DataResult.success(Enum.valueOf(enumType, s.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(e.getMessage());
            }
        }, e -> e.name().toLowerCase(Locale.ROOT));
    }

    private ExtraCodecs() {
    }
}
