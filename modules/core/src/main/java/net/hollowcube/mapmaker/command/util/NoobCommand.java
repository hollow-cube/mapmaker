package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.util.CoreSkulls;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.UUID;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class NoobCommand extends CommandDsl {
    private static final ItemStack NOOB_HELMET = ItemStack.builder(Material.PLAYER_HEAD)
        .set(DataComponents.PROFILE, CoreSkulls.ROBLOX_NOOB)
        .set(DataComponents.CUSTOM_NAME, Component.text("Roblox Noob", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false))
        .build();

    private static final ItemStack NOOB_CHESTPLATE = ItemStack.of(Material.LEATHER_CHESTPLATE);
    private static final ItemStack NOOB_LEGGINGS = ItemStack.of(Material.LEATHER_LEGGINGS);
    private static final ItemStack NOOB_BOOTS = ItemStack.of(Material.LEATHER_BOOTS);

    public NoobCommand() {
        super("noob");

        category = CommandCategories.STAFF;
        description = "Become the Roblox noob!";

        setCondition(staffPerm(Permission.GENERIC_STAFF));
        addSyntax(playerOnly(this::handleApplyNoob));
    }

    private void handleApplyNoob(Player player, CommandContext context) {
        player.setHelmet(NOOB_HELMET);
        player.setChestplate(NOOB_CHESTPLATE);
        player.setLeggings(NOOB_LEGGINGS);
        player.setBoots(NOOB_BOOTS);
        player.sendMessage(Component.text("You are now a Roblox Noob!", NamedTextColor.BLUE));
    }

    private static UUID intArrayToUuid(int[] array) {
        final long uuidMost = (long) array[0] << 32 | array[1] & 0xFFFFFFFFL;
        final long uuidLeast = (long) array[2] << 32 | array[3] & 0xFFFFFFFFL;

        return new UUID(uuidMost, uuidLeast);
    }
}
