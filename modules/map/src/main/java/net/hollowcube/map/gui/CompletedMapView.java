package net.hollowcube.map.gui;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class CompletedMapView extends ParentSection {

    public CompletedMapView() {
        super(9, 1);

        add(0, new ButtonSection(1, 1, ItemStack.of(Material.PAPER).withDisplayName(Component.text("Map complete"))));
    }
}
