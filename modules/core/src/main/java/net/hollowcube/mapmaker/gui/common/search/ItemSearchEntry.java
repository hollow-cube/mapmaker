package net.hollowcube.mapmaker.gui.common.search;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.util.ItemUtils;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class ItemSearchEntry extends View {

    public static final String SIGNAL = "search.selected";

    private @Outlet("label") Label label;
    private final @NotNull Material material;

    public ItemSearchEntry(@NotNull Context context, @NotNull Material material) {
        super(context);

        this.material = material;

        this.label.setItemSprite(ItemUtils.asDisplay(material));
        this.label.setArgs(LanguageProviderV2.getVanillaTranslation(material));
    }

    @Action("label")
    private void handleSelect() {
        performSignal(SIGNAL, this.material);
    }


}
