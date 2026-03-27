package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.command.CommandCondition.hideOnClient;
import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class AddInteractionCommand extends CommandDsl {

    public AddInteractionCommand() {
        super("addinteraction");

        this.category = CommandCategory.HIDDEN;

        setCondition(and(hideOnClient(), builderOnly()));

        addSyntax(playerOnly(this::addInteractionEntity));
    }

    private void addInteractionEntity(Player player, CommandContext context) {
        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;

        var entity = new InteractionEntity();
        entity.setBoundingBox(1, 1, 1);
        entity.setInstance(world.instance(), player.getPosition().withView(Pos.ZERO));
        player.sendMessage(Component.text("Interaction added.")
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy ID")))
                .clickEvent(ClickEvent.copyToClipboard(entity.getUuid().toString())));
    }
}
