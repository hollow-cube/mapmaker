package net.hollowcube.terraform.tool;

import com.google.auto.service.AutoService;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.session.LocalSession;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

@AutoService(BuiltinTool.class)
public class WandTool implements BuiltinTool {
    private static final Key TYPE = Key.key("terraform:wand");

    @Override
    public @NotNull Key key() {
        return TYPE;
    }

    @Override
    public int flags() {
        return RIGHT_CLICK_BLOCK | LEFT_CLICK_BLOCK;
    }

    @Override
    public @NotNull Material material() {
        return Material.WOODEN_AXE;
    }

    @Override
    public void leftClicked(@NotNull Click click) {
        // Determine the target selection
        var player = click.player();
        var session = LocalSession.forPlayer(player);
        var selection = session.selection(Selection.DEFAULT);
        //todo all tools should have a selection tag to set their selection explicitly

        // Update the selection
        var blockPosition = click.blockPosition();
        var changed = selection.selectPrimary(blockPosition, true);
        if (!changed) {
            player.sendMessage(Component.translatable("terraform.pos1.already_set"));
        }
    }

    @Override
    public void rightClicked(@NotNull Click click) {
        // Determine the target selection
        var player = click.player();
        var session = LocalSession.forPlayer(player);
        var selection = session.selection(Selection.DEFAULT);

        // Update the selection
        var blockPosition = click.blockPosition();
        var changed = selection.selectSecondary(blockPosition, true);
        if (!changed) {
            player.sendMessage(Component.translatable("terraform.pos2.already_set"));
        }
    }

}
