package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class MapIconPreview extends View {

    private @Outlet("label") Label label;

    public MapIconPreview(@NotNull Context context, @NotNull Material material) {
        super(context);
        label.setItemSprite(ItemStack.of(material));
        label.setArgs(Component.text(material.name()));
    }

}
