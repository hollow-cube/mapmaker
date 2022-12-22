package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.std.AnvilSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AnvilInputDemo extends AnvilSection {

    public AnvilInputDemo() {
        add(0, new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND_SWORD)
                .withDisplayName(Component.text("abc"))
                .withLore(List.of(Component.text("first line"))), () -> {
            System.out.println("Clicked the paper!");
        }));
        add(1, new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND_SWORD)
                .withDisplayName(Component.text("def"))
                .withLore(List.of(Component.text("second line")))
                .withMeta(meta -> meta.customModelData(5)), () -> {
            System.out.println("Clicked the paper!");
        }));
        add(2, new ButtonSection(1, 1, ItemStack.of(Material.PAPER), () -> {
            System.out.println("Clicked the paper!");
        }));
    }

    public void onInput(@NotNull String input) {
        System.out.println("Input: " + input);
    }

}
