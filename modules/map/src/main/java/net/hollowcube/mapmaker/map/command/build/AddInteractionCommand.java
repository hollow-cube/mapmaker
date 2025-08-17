package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.command.arg.CoreCondition.feature;
import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class AddInteractionCommand extends CommandDsl {

    public AddInteractionCommand() {
        super("addinteraction");

        setCondition(and(
                mapFilter(false, true, false),
                feature(MapFeatureFlags.MARKER_TOOL)
        ));

        addSyntax(playerOnly(this::addInteractionEntity));
    }

    private void addInteractionEntity(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);

        var entity = new InteractionEntity();
        entity.setInstance(world.instance(), player.getPosition().withView(Pos.ZERO));
        player.sendMessage(Component.text("Interaction added.")
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                .clickEvent(ClickEvent.copyToClipboard(entity.getUuid().toString())));
    }
}
