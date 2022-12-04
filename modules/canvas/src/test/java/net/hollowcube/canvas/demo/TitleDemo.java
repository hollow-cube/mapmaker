package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class TitleDemo extends ParentSection {
    public TitleDemo() {
        super(9, 1);

        add(0, new ButtonSection(1, 1, ItemStack.of(Material.PAPER), () -> {
            find(RootSection.class).setTitle(Component.text("One"));
        }));

        add(1, new ButtonSection(1, 1, ItemStack.of(Material.PAPER), () -> {
            find(RootSection.class).setTitle(Component.text("Two"));
        }));

        add(2, new ButtonSection(1, 1, ItemStack.of(Material.PAPER), () -> {
            find(RootSection.class).setTitle(Component.text("Three"));
        }));
    }

    @Override
    protected void mount() {
        super.mount();
        find(RootSection.class).setTitle(Component.text("Title Demo"));
    }
}
