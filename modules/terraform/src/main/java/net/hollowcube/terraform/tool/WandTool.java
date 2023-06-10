package net.hollowcube.terraform.tool;

import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WandTool {
    private static final String TYPE = "terraform:wand";
    private static final Component DEFAULT_NAME = Component.translatable("tool.terraform.wand.name");

    public void leftClickedBlock(@NotNull Player player, @NotNull Point blockPosition) {
        // Determine the target selection
        var session = LocalSession.forPlayer(player);
        var selection = session.selection(Selection.DEFAULT);
        //todo all tools should have a selection tag to set their selection explicitly

        // Update the selection
        var changed = selection.selectPrimary(blockPosition, true);
        if (!changed) {
            player.sendMessage(Component.translatable("command.terraform.pos1.already_set"));
        }
    }

    public void rightClickedBlock(@NotNull Player player, @NotNull Point blockPosition) {
        // Determine the target selection
        var session = LocalSession.forPlayer(player);
        var selection = session.selection(Selection.DEFAULT);

        // Update the selection
        var changed = selection.selectSecondary(blockPosition, true);
        if (!changed) {
            player.sendMessage(Component.translatable("command.terraform.pos2.already_set"));
        }
    }

    /*

    tags:
    - tool/name
    - tool/type


    Name: Wand Tool (or player overridable with /tool name <name>, stored as tag)

    $LMB to select primary position
    $RMB to select secondary position

     */

}
