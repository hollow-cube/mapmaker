package net.hollowcube.terraform.compat.worldedit.wand;

import net.hollowcube.terraform.session.Session;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

/**
 * Implements the position selecting axe. This should eventually be replaced with a command binding feature (eg bind hpos1/hpos2 to l/r click)
 */
public class WandHandler {

    public static EventNode<PlayerEvent> EVENT_NODE = EventNode.type("tf_we:wand", EventFilter.PLAYER)
            .addListener(PlayerBlockBreakEvent.class, WandHandler::leftClicked)
            .addListener(PlayerUseItemOnBlockEvent.class, WandHandler::rightClicked);

    private static void leftClicked(@NotNull PlayerBlockBreakEvent event) {
        var itemStack = event.getPlayer().getItemInMainHand();
        if (!isWandItem(itemStack)) return;
        event.setCancelled(true);

        var player = event.getPlayer();
        var selector = Session.forPlayer(player).getRegionSelector(player.getInstance());

        var pos = event.getBlockPosition();
        if (selector.selectPrimary(pos)) {
            player.sendMessage(MessageFormat.format("Pos1 set to {0},{1},{2}", pos.blockX(), pos.blockY(), pos.blockZ()));
        }
    }

    private static void rightClicked(@NotNull PlayerUseItemOnBlockEvent event) {
        var itemStack = event.getItemStack();
        if (!isWandItem(itemStack)) return;

        var player = event.getPlayer();
        var selector =  Session.forPlayer(player).getRegionSelector(player.getInstance());

        var pos = event.getPosition();
        var changed = selector.selectSecondary(pos);
        if (changed) {
            player.sendMessage(MessageFormat.format("Pos2 set to {0},{1},{2}", pos.blockX(), pos.blockY(), pos.blockZ()));
        }
    }

    private static boolean isWandItem(@NotNull ItemStack itemStack) {
        return itemStack.material() == Material.WOODEN_AXE;
    }


}
