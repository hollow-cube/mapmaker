package net.hollowcube.mapmaker.map.hdb;

import com.miguelfonseca.completely.data.Indexable;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.PlayerHeadMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public record HeadInfo(
        @NotNull String id,
        @NotNull String name,
        @NotNull String category,
        @NotNull String texture,
        @NotNull List<String> tags
) implements Indexable {
    public static @Nullable HeadInfo fromLine(String line) {
        var parts = line.split(";");
        if (parts.length < 4) return null;

        var texture = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + parts[3] + "\"}}}";
        texture = Base64.getEncoder().encodeToString(texture.getBytes(StandardCharsets.UTF_8));

        List<String> tags = parts.length >= 5 ? List.of(parts[4].split("\\|")) : List.of();
        return new HeadInfo(parts[1], parts[2], parts[0], texture, tags);
    }

    public @NotNull ItemStack createItemStack() {
        return ItemStack.builder(Material.PLAYER_HEAD)
                .meta(PlayerHeadMeta.class, meta -> meta.playerSkin(new PlayerSkin(texture, null)))
                .displayName(LanguageProviderV2.translate(HdbMessages.ITEM_HDB_HEAD_NAME.with(name)))
                .lore(LanguageProviderV2.translateMulti("item.hdb.head.lore", List.of(
                        Component.text(id), Component.translatable("hdb.category." + category + ".name"),
                        Component.text(String.join(", ", tags))
                )))
                .build();
    }

    /**
     * Returns the indexed fields of this head.
     *
     * @return the indexed fields
     */
    @Override
    public List<String> getFields() {
        var fields = new ArrayList<String>();
        fields.add(id);
        fields.add(name);
        fields.addAll(tags);
        return fields;
    }
}
