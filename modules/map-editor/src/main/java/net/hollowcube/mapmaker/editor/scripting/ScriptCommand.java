package net.hollowcube.mapmaker.editor.scripting;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;

public class ScriptCommand extends CommandDsl {

    public ScriptCommand() {
        super("script");

        setCondition(builderOnly());

        addSubcommand(new Editor());
    }

    private static final class Editor extends CommandDsl {

        private Editor() {
            super("editor");

            addSyntax(playerOnly(this::execute));
        }

        private void execute(Player player, CommandContext context) {
            var world = EditorMapWorld.forPlayer(player);
            if (world == null) return; // sanity

            var playerId = PlayerData.fromPlayer(player).id();
            var mapId = world.map().id();

            var grant = world.server().api().auth.createLaunchGrant(playerId, mapId);

            player.sendMessage(Component.text("Click to open editor", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.openUrl(grant.url())));
        }
    }
}
