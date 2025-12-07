package net.hollowcube.mapmaker.editor.command.utility;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.SpectateHelper;
import net.minestom.server.entity.Player;

import static net.hollowcube.command.CommandCondition.or;
import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.kyori.adventure.text.Component.translatable;

public class FlyCommand extends CommandDsl {

    public FlyCommand() {
        super("fly");

        setCondition(or(builderOnly(), playingOrSpectatingFilter()));

        addSyntax(playerOnly(this::handleToggleFly));
    }

    private void handleToggleFly(Player player, CommandContext context) {

        var world = ParkourMapWorld.forPlayer(player);
        if (world != null) {
            SpectateHelper.toggleSpectatorFlight(world, player);
            return;
        }
        var editWorld = EditorMapWorld.forPlayer(player);
        if (editWorld != null) {
            boolean canFly = player.isAllowFlying();
            player.setAllowFlying(!canFly);
            player.setFlying(!canFly);

            player.sendMessage(translatable(!canFly ? "command.fly.enabled" : "command.fly.disabled"));
        }
    }

    // This should not be in the editor module but i dont have an immediate way to move it
    static CommandCondition playingOrSpectatingFilter() {
        return (sender, _) -> {
            if (!(sender instanceof Player player))
                return CommandCondition.HIDE;

            var world = ParkourMapWorld.forPlayer(player);
            if (world == null) return CommandCondition.HIDE;

            return world.getPlayerState(player) instanceof ParkourState.Spectating
                    ? CommandCondition.ALLOW : CommandCondition.HIDE;
        };
    }
}
