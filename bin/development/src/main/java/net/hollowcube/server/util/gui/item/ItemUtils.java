package net.hollowcube.server.util.gui.item;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemHideFlag;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class ItemUtils {
    // Default player hotbar
    private static final Component NAME_PLAY_MAPS =
            Component.text("Play Maps ", NamedTextColor.BLUE)
                    .append(Component.text("(", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Right Click", NamedTextColor.GRAY))
                    .append(Component.text(")", NamedTextColor.DARK_GRAY));

    private static final Component NAME_CREATE_MAPS =
            Component.text("Create Maps ", NamedTextColor.DARK_AQUA)
                    .append(Component.text("(", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Right Click", NamedTextColor.GRAY))
                    .append(Component.text(")", NamedTextColor.DARK_GRAY));

    private static final Component[] LORE_PLAY_MAPS = {
            Component.text("Browse a large catalog of unique", NamedTextColor.GRAY),
            Component.text("maps, all created by players!", NamedTextColor.GRAY)
    };

    private static final Component[] LORE_CREATE_MAPS = {
            Component.text("Create your very own maps and", NamedTextColor.GRAY),
            Component.text("publish them for all players to play!", NamedTextColor.GRAY)
    };

    public static final ItemStack GUI_PLAY_MAPS =
            ItemStack.builder(Material.NETHER_STAR)
                    .displayName(NAME_PLAY_MAPS)
                    .lore(LORE_PLAY_MAPS[0], LORE_PLAY_MAPS[1])
                    .build();

    public static final ItemStack GUI_CREATE_MAPS =
            ItemStack.builder(Material.DIAMOND_PICKAXE)
                    .displayName(NAME_CREATE_MAPS)
                    .lore(LORE_CREATE_MAPS[0], LORE_CREATE_MAPS[1])
                    .meta(metabuilder -> metabuilder.hideFlag(ItemHideFlag.HIDE_ATTRIBUTES))
                    .build();
}
