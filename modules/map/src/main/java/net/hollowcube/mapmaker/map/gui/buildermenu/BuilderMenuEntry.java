package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BuilderMenuEntry extends View {

    private @Outlet("label") Label label;

    protected BuilderMenuEntry(
            @NotNull Context context,
            @NotNull BuilderMenuTabItems.Item item
    ) {
        super(context);

        var material = item.icon().right().orElse(null);
        var sprite = item.icon().left().orElse(null);

        if (material != null) {
            this.label.setItemSprite(ItemStack.of(material));
        } else if (sprite != null) {
            if (sprite.fontChar() != 0) {
                this.label.setSprite(sprite.fontChar(), sprite.model(), sprite.width(), sprite.offsetX(), sprite.rightOffset());
            } else {
                this.label.setItemSprite(ItemStack.builder(Material.DIAMOND).set(DataComponents.ITEM_MODEL, sprite.model()).build());
            }
        }

        this.label.setComponentsDirect(
                Component.translatable(String.format("%s.name", item.translation())),
                LanguageProviderV2.translateMulti(String.format("%s.lore", item.translation()), List.of())
        );

        this.addActionHandler("label", Label.ActionHandler.lmb(item::give));
    }
}
