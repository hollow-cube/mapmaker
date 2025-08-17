package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BuilderMenuEntry extends View {

    private @Outlet("label") Label label;

    protected BuilderMenuEntry(
            @NotNull Context context,
            @NotNull BuilderMenuTabItems.Item item
    ) {
        super(context);

        var material = item.icon().rightOr(null);
        var sprite = item.icon().leftOr(null);

        if (sprite != null && sprite.fontChar() != 0) {
            this.label.setSprite(sprite.fontChar(), sprite.model(), sprite.width(), sprite.offsetX(), sprite.rightOffset());
        } else {
            var locked = !item.canGive(context.player());
            if (material != null) {
                this.label.setItemSprite(ItemUtils.asDisplay(material, locked ? "lock" : null));
            } else if (sprite != null) {
                this.label.setItemSprite(ItemStack
                        .builder(Material.DIAMOND)
                        .set(DataComponents.ITEM_MODEL, sprite.model())
                        .set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(
                                List.of(),
                                List.of(),
                                locked ? List.of("lock") : List.of(),
                                List.of()
                        ))
                        .build()
                );
            }
        }

        var loreTranslation = item.canGive(context.player()) ?
                String.format("%s.lore", item.translation()) :
                String.format("%s.disabled.lore", item.translation());

        this.label.setComponentsDirect(
                Component.translatable(String.format("%s.name", item.translation())),
                LanguageProviderV2.translateMulti(loreTranslation, List.of())
        );

        this.addActionHandler("label", Label.ActionHandler.lmb(item::give));
    }
}
