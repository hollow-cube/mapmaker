package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class AddMarkerCommand extends CommandDsl {
    private final Argument<String> typeArg = Argument.Word("type")
            .defaultValue("mapmaker:checkpoint")
            .description("The type of marker to add (default: mapmaker:checkpoint)");

    public AddMarkerCommand() {
        super("addmarker");

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::addMarkerEntity));
        addSyntax(playerOnly(this::addMarkerEntity), typeArg);
    }

    private void addMarkerEntity(@NotNull Player player, @NotNull CommandContext context) {
        @Subst("mapmaker:checkpoint") var type = context.get(typeArg);
        if (!Key.parseable(type)) {
            player.sendMessage(Component.text("Invalid marker type: " + type));
            return;
        }

        var world = MapWorld.forPlayer(player);

        var entity = new MarkerEntity();
        entity.setType(Key.key(type));
        entity.setInstance(world.instance(), player.getPosition().withView(Pos.ZERO));
        player.sendMessage(Component.text("Marker added.")
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                .clickEvent(ClickEvent.copyToClipboard(entity.getUuid().toString())));
    }

}
