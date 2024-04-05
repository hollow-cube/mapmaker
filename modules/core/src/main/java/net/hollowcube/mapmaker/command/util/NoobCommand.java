package net.hollowcube.mapmaker.command.util;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.PlayerHeadMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NoobCommand extends CommandDsl {
    private static final ItemStack NOOB_HELMET = ItemStack.builder(Material.PLAYER_HEAD)
            .meta(PlayerHeadMeta.class, meta -> {
                meta.playerSkin(new PlayerSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGEyODRkOTc4MDViZTUzMWNhZGYwNjI3YzVjMjllOTAzNWUxNzEyMTU4MWRjYWJjZjk1MTBmZmQ5ZDQ2MDdmZiJ9fX0=", null));
                meta.skullOwner(intArrayToUuid(new int[]{67411088, -739686879, -1666252800, -1432128361}));
            })
            .displayName(Component.text("Roblox Noob", NamedTextColor.BLUE).decoration(TextDecoration.ITALIC, false))
            .build();

    private static final ItemStack NOOB_CHESTPLATE = ItemStack.of(Material.LEATHER_CHESTPLATE);
    private static final ItemStack NOOB_LEGGINGS = ItemStack.of(Material.LEATHER_LEGGINGS);
    private static final ItemStack NOOB_BOOTS = ItemStack.of(Material.LEATHER_BOOTS);

    @Inject
    public NoobCommand(@NotNull PermManager permManager) {
        super("noob");

        category = CommandCategories.STAFF;
        description = "Become the Roblox noob!";

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
        addSyntax(playerOnly(this::handleApplyNoob));
    }

    private void handleApplyNoob(@NotNull Player player, @NotNull CommandContext context) {
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
