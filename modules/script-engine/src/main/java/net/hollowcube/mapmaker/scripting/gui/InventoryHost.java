package net.hollowcube.mapmaker.scripting.gui;

import net.hollowcube.mapmaker.scripting.gui.node.Node;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InventoryHost {
    public Node root; // TODO: do we need multiple roots?

    public void addChild(@NotNull Node node) {
        if (this.root != null) {
            throw new IllegalStateException("The root of a GUI must be a single element");
        }

        this.root = node;
    }

    public Component titleTemp;
    public ItemStack[] itemsTemp;

    public ItemStack[] build() {
        MenuBuilder builder = new MenuBuilder(9, 10);
        this.root.build(builder);
        itemsTemp = builder.getItems();
        titleTemp = builder.getTitle();
        return itemsTemp;
    }
}
