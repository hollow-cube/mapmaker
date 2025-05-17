package net.hollowcube.mapmaker.map.hdb;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.RuntimeGson;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RuntimeGson
public record HeadInfo(
        @NotNull String id,
        @NotNull String name,
        @NotNull String category,
        @NotNull String texture,
        @NotNull List<String> tags
) {
    public HeadInfo {
        var skinTexture = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + texture + "\"}}}";
        texture = Base64.getEncoder().encodeToString(skinTexture.getBytes(StandardCharsets.UTF_8));
    }

    public @NotNull ItemStack createItemStack() {
        return ItemStack.builder(Material.PLAYER_HEAD)
                .set(DataComponents.PROFILE, new HeadProfile(new PlayerSkin(texture, null)))
                .set(DataComponents.CUSTOM_NAME, LanguageProviderV2.translate(HdbMessages.ITEM_HDB_HEAD_NAME.with(name)))
                .set(DataComponents.LORE, LanguageProviderV2.translateMulti("item.hdb.head.lore", List.of(
                        Component.text(id), Component.translatable("hdb.category." + category + ".name"),
                        Component.text(String.join(", ", tags))
                )))
                .build();
    }
}
