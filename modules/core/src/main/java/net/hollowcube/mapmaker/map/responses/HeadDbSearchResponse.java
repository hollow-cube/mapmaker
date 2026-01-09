package net.hollowcube.mapmaker.map.responses;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.util.CoreSkulls;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@RuntimeGson
public record HeadDbSearchResponse(
        Integer pages,
        List<HeadInfo> results
) {

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
                .set(DataComponents.PROFILE, CoreSkulls.create(new PlayerSkin(texture, null)))
                .set(DataComponents.CUSTOM_NAME, Component.text(name)
                    .decoration(TextDecoration.ITALIC, false)
                    .color(NamedTextColor.WHITE)
                )
                .set(DataComponents.LORE, LanguageProviderV2.translateMulti("item.hdb.head.lore", List.of(
                    Component.text(id), Component.translatable("hdb.category." + category + ".name"),
                    Component.text(String.join(", ", tags))
                )))
                .build();
        }
    }
}
