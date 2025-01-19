package net.hollowcube.mapmaker.map.gui.displayentity.object;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.util.TagUtil;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class ObjectEntry extends View {

    public static final String SIGNAL = "display_icon.selected";

    private @Outlet("label") Label label;
    private final @NotNull Material material;

    public ObjectEntry(@NotNull Context context, @NotNull Material material) {
        super(context);

        this.material = material;

        var iconItem = ItemStack.builder(material);
        TagUtil.removeTooltipExtras(iconItem);
        this.label.setItemSprite(iconItem.build());
        this.label.setArgs(LanguageProviderV2.getVanillaTranslation(material));
    }

    @Action("label")
    private void handleSelect() {
        performSignal(SIGNAL, this.material);
    }


}
