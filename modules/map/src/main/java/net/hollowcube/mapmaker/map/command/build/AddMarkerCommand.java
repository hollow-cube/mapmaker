package net.hollowcube.mapmaker.map.command.build;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.map.util.MapCondition.mapFeature;
import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class AddMarkerCommand extends CommandDsl {

    @Inject
    public AddMarkerCommand() {
        super("addmarker");

        setCondition(and(
                mapFilter(false, true, false),
                mapFeature(MapFeatureFlags.MARKER_TOOL)
        ));

        addSyntax(playerOnly(this::addMarkerEntity));
    }

    private void addMarkerEntity(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);

        var entity = new MarkerEntity();
        entity.setType(NamespaceID.from("mapmaker:test"));
        entity.setInstance(world.instance(), player.getPosition().withView(Pos.ZERO));
        player.sendMessage(Component.text("Marker added.")
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                .clickEvent(ClickEvent.copyToClipboard(entity.getUuid().toString())));
    }

}
