package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.PlayerUtil;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class BuildToolsView extends View {

    public BuildToolsView(@NotNull Context context) {
        super(context);
    }

    @Action("give_debug_stick")
    private void giveDebugStick(@NotNull Player player) {
        PlayerUtil.smartAddItemStack(player, ItemStack.of(Material.DEBUG_STICK));
        player.closeInventory();
    }

    @Action("give_wand")
    private void giveWand(@NotNull Player player) {
        var tf = LocalSession.forPlayer(player).terraform();
        var itemStack = tf.toolHandler().createBuiltinTool("terraform:wand");

        PlayerUtil.smartAddItemStack(player, itemStack);
        player.closeInventory();
    }
}
