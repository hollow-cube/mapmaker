package net.hollowcube.terraform.command.terraform;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.terraform.session.PlayerSession;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TerraformToggleCuiCommand extends CommandDsl {
    public TerraformToggleCuiCommand() {
        super("cui");
        description = "Toggles the default cui implementation.";

        addSyntax(playerOnly(this::execute));
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var playerData = PlayerDataV2.fromPlayer(player);
        boolean setting = playerData.getSetting(PlayerSettings.ENABLE_WE_CUI);
        playerData.setSetting(PlayerSettings.ENABLE_WE_CUI, !setting);
        //todo save

        if (!setting) {
            player.sendMessage(Component.translatable("terraform.command.tf.cui.on"));
        } else {
            player.sendMessage(Component.translatable("terraform.command.tf.cui.off"));
        }

        PlayerSession.forPlayer(player).updateRenderer();
    }
}
