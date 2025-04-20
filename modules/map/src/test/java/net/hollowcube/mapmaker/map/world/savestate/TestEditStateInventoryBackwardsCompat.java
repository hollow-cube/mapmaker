package net.hollowcube.mapmaker.map.world.savestate;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestEditStateInventoryBackwardsCompat {
    private static final Codec<Map<Integer, ItemStack>> MODERN_CODEC = EditState.INVENTORY_CODEC;

    @Test
    void testEmpty() {
        assertEquals(Map.of(), decodeJson(MODERN_CODEC, "{}"));
    }

    @Test
    void testMissing() {
        record Parent(Map<Integer, ItemStack> inventory) {
        }
        Codec<Parent> parentCodec = RecordCodecBuilder.create(i -> i.group(
                MODERN_CODEC.optionalFieldOf("inventory", Map.of()).forGetter(Parent::inventory)
        ).apply(i, Parent::new));
        assertEquals(Map.of(), decodeJson(parentCodec, "{}").inventory);
    }

    @Test
    void testReadModernSimple() {
        var result = decodeJson(MODERN_CODEC, """
                {"0": {"id": "minecraft:stone"}}
                """);
        assertEquals(Map.of(0, ItemStack.of(Material.STONE)), result);
    }

    @Test
    void testReadModernComponent() {
        var expectedItem = ItemStack.builder(Material.STONE)
                .set(DataComponents.CUSTOM_NAME, Component.text("test"))
                .amount(44)
                .build();
        var result = decodeJson(MODERN_CODEC, """
                {"33": {"id": "minecraft:stone", "count": 44, "components": {"minecraft:custom_name": "test"}}}
                """);
        assertEquals(Map.of(33, expectedItem), result);
    }

    @Test
    void testReadLegacyA() {
        var result = decodeJson(MODERN_CODEC, "\"AQAKCAACaWQAD21pbmVjcmFmdDpnbGFzcwEABUNvdW50AQA=\"");
        assertEquals(Map.of(0, ItemStack.of(Material.GLASS)), result);
    }

    private <T> @NotNull T decodeJson(@NotNull Codec<T> codec, @NotNull String json) {
        class Holder {
            static final Gson gson = new Gson();
        }
        return codec.parse(JsonOps.INSTANCE, Holder.gson.fromJson(json, JsonElement.class)).getOrThrow();
    }
}
