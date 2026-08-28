package net.hollowcube.mapmaker.editor.hdb;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.ipc.hdb.HeadInfo;
import net.hollowcube.mapmaker.util.CoreSkulls;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNullByDefault;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/// Turns a [HeadInfo] into the head a player can hold.
///
/// The database stores a texture hash; a profile wants the base64 of the JSON document naming it,
/// so that encoding happens here rather than in the record, which crosses the wire.
@NotNullByDefault
public final class HeadItems {

    public static ItemStack createItemStack(HeadInfo head) {
        return ItemStack.builder(Material.PLAYER_HEAD)
            .set(DataComponents.PROFILE, CoreSkulls.create(new PlayerSkin(profileTexture(head.texture()), null)))
            .set(DataComponents.CUSTOM_NAME, Component.text(head.name())
                .decoration(TextDecoration.ITALIC, false)
                .color(NamedTextColor.WHITE)
            )
            .set(DataComponents.LORE, LanguageProviderV2.translateMulti("item.hdb.head.lore", List.of(
                Component.text(head.id()), Component.translatable("hdb.category." + head.category() + ".name"),
                Component.text(String.join(", ", head.tags()))
            )))
            .build();
    }

    private static String profileTexture(String hash) {
        var document = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + hash + "\"}}}";
        return Base64.getEncoder().encodeToString(document.getBytes(StandardCharsets.UTF_8));
    }

    private HeadItems() {
    }
}
