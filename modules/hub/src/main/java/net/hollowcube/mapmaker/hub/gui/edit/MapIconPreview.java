package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class MapIconPreview extends View {
    public static final String SIG_SELECTED = "map_icon_preview.selected";

    private @Outlet("label") Label label;

    private final Material material;
    private final String labelText;

    public MapIconPreview(@NotNull Context context, @NotNull Material material) {
        super(context);
        this.material = material;
        this.labelText = material.name();

        label.setItemSprite(ItemStack.of(material));
        label.setArgs(Component.text(labelText));
    }

    public MapIconPreview(@NotNull Context context) {
        super(context);
        this.material = Material.BARRIER;
        this.labelText = "No Results Found!";

        label.setItemSprite(ItemStack.of(Material.BARRIER));
        label.setArgs(Component.text(labelText));
    }

    @Action("label")
    private void handleSelect() {
        if (labelText.equals("No Results Found!")) {
            return;
        }
        performSignal(SIG_SELECTED, material);
    }
}
