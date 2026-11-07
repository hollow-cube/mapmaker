package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.runtime.parkour.marker.TriggerMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerTemplate;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.intellij.lang.annotations.Subst;

import java.util.TreeSet;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.kyori.adventure.text.Component.text;

public class AddMarkerCommand extends CommandDsl {
    private final Argument<String> typeArg = Argument.Word("type")
            .description("The type of marker to add");

    public AddMarkerCommand() {
        super("addmarker");

        setCondition(and(builderOnly()));

        addSyntax(playerOnly(this::sendMarkerTypes));
        addSyntax(playerOnly(this::addMarkerEntity), typeArg);
    }

    private void sendMarkerTypes(Player player, CommandContext context) {
        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;

        var types = new TreeSet<>(world.objectEntityHandlers().markerTypes());
        types.addAll(TriggerTemplate.PRESETS.keySet());

        var message = text();
        message.append(text("Specify a marker type: /addmarker <type>. Available types: "));
        for (var type : types) {
            message.appendNewline();
            message.append(text(" - " + type));
        }
        player.sendMessage(message);
    }

    private void addMarkerEntity(Player player, CommandContext context) {
        @Subst("mapmaker:checkpoint") var type = context.get(typeArg);
        if (!Key.parseable(type)) {
            player.sendMessage(text("Invalid marker type: " + type));
            return;
        }

        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;

        var entity = new MarkerEntity();
        var preset = TriggerTemplate.PRESETS.get(type);
        if (preset != null) {
            entity.setType(Key.key(TriggerMarkerHandler.ID));
            entity.setData(preset.mergeInto(entity.getData()), null, false);
        } else {
            entity.setType(Key.key(type));
        }
        entity.setInstance(world.instance(), player.getPosition().withView(Pos.ZERO));
        player.sendMessage(text("Marker added.")
                .hoverEvent(HoverEvent.showText(text("Click to copy ID")))
                .clickEvent(ClickEvent.copyToClipboard(entity.getUuid().toString())));
    }

}
