package net.hollowcube.aj.entity;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.arguments.minecraft.ArgumentBlockState;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.BlockDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class AnimationEntityTypes {
    private static final Map<String, Function<JsonObject, AnimationEntity>> ENTITY_TYPES = Map.of(
            "block_display", BlockDisplay::new,
            "item_display", ItemDisplay::new,
            "text_display", TextDisplay::new
    );

    public static @NotNull AnimationEntity parse(@NotNull JsonObject data) {
        var type = data.get("type").getAsString();
        var factory = Objects.requireNonNull(ENTITY_TYPES.get(type),
                () -> "no such entity type: " + type);
        return factory.apply(data);
    }

    public static class BlockDisplay extends AnimationEntity {

        public BlockDisplay(@NotNull JsonObject data) {
            super(EntityType.BLOCK_DISPLAY, data);

            var blockName = data.get("block").getAsString();
            var block = ArgumentBlockState.staticParse(blockName);
            getEntityMeta().setBlockState(Objects.requireNonNull(block));
        }

        @Override
        public @NotNull BlockDisplayMeta getEntityMeta() {
            return (BlockDisplayMeta) super.getEntityMeta();
        }
    }

    public static class ItemDisplay extends AnimationEntity {

        public ItemDisplay(@NotNull JsonObject data) {
            super(EntityType.ITEM_DISPLAY, data);

            var itemName = data.get("item");
            if (itemName != null) {
                var material = Objects.requireNonNull(Material.fromNamespaceId(itemName.getAsString()));
                getEntityMeta().setItemStack(ItemStack.of(material));
            }
        }

        @Override
        public @NotNull ItemDisplayMeta getEntityMeta() {
            return (ItemDisplayMeta) super.getEntityMeta();
        }
    }

    public static class TextDisplay extends AnimationEntity {

        public TextDisplay(@NotNull JsonObject data) {
            super(EntityType.TEXT_DISPLAY, data);

            final var meta = getEntityMeta();
            meta.setText(Component.text(data.get("text").getAsString()));
        }

        @Override
        public @NotNull TextDisplayMeta getEntityMeta() {
            return (TextDisplayMeta) super.getEntityMeta();
        }
    }

    private AnimationEntityTypes() {
    }

}
