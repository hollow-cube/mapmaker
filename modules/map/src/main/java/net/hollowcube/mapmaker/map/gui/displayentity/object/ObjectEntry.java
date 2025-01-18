package net.hollowcube.mapmaker.map.gui.displayentity.object;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.util.TagUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ObjectEntry extends View {

    public static final String SIGNAL = "display_icon.selected";

    private @Outlet("label") Label label;
    private final @Nullable Material material;

    public ObjectEntry(@NotNull Context context, @Nullable Material material) {
        super(context);

        this.material = material;

        if (this.material != null) {
            var iconItem = ItemStack.builder(material);
            TagUtil.removeTooltipExtras(iconItem);
            this.label.setItemSprite(iconItem.build());
            this.label.setArgs(LanguageProviderV2.getVanillaTranslation(material));
        } else {
            this.label.setArgs(Component.translatable("gui.display_entity.no_results"));
        }
    }

    @Action("label")
    private void handleSelect() {
        if (this.material == null) return;
        performSignal(SIGNAL, this.material);
    }


}
