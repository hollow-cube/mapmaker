package net.hollowcube.mapmaker.editor.command.navigation;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class BackCommand extends CommandDsl {
    public static final Tag<Point> LAST_LOCATION = Tag.Transient("teleport_history:last_location");

    private static final Component NO_PREVIOUS_LOCATION = Component.translatable("commands.back.no_previous_location");
    private static final Component SUCCESS = Component.translatable("commands.back.success");

    public BackCommand() {
        super("back");

        category = CommandCategories.MAP;
        description = "Teleports you back to your last location you teleported from.";

        setCondition(builderOnly());
        addSyntax(playerOnly(this::execute));
    }

    private void execute(Player player, CommandContext context) {
        var lastLocation = player.getTag(LAST_LOCATION);
        if (lastLocation == null) {
            player.sendMessage(NO_PREVIOUS_LOCATION);
        } else {
            MapWorldHelpers.teleportPlayer(player, lastLocation).thenRun(() -> player.sendMessage(SUCCESS));
        }
    }
}

