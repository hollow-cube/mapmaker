package net.hollowcube.mapmaker.runtime.parkour.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.SpectateHelper;
import net.kyori.adventure.util.TriState;
import net.minestom.server.entity.Player;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.map.command.MapConditions.mapPlayer;
import static net.hollowcube.mapmaker.runtime.parkour.command.ParkourConditions.parkourWorld;

public class SpectateCommand extends CommandDsl {

    public SpectateCommand() {
        super("spectate", "spec", "practice", "prac");

        category = CommandCategories.MAP;
        description = "Spectate the current map";

        setCondition(and(
                parkourWorld(),
                mapPlayer((world, player) -> world instanceof ParkourMapWorld parkour && SpectateHelper.canSpectate(parkour, player))
        ));
        addSyntax(playerOnly(this::execute));
    }

    private void execute(Player player, CommandContext context) {
        SpectateHelper.changeSpecState(player, TriState.NOT_SET);
    }

}
