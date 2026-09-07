package net.hollowcube.mapmaker.command.playerinfo;

import java.util.UUID;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

class AltsInfoType implements PlayerInfoType<String> {

    private final PlayerService players;

    public AltsInfoType(@NotNull PlayerService players) {
        this.players = players;
    }

    @Override
    public Argument<String> getArgument() {
        return CoreArgument.AnyPlayerId("player", players);
    }

    @Override
    public void execute(@NotNull Player user, @NotNull String target) {
        var alts = players.alts(UUID.fromString(target));
        if (alts.isEmpty()) {
            user.sendMessage("No alts found for %s".formatted(target));
        } else {
            var component = text()
                .append(text("Alts for "))
                .append(players.displayName(UUID.fromString(target)).render())
                .append(text(":"))
                .appendNewline();
            for (var alt : alts) {
                component = component.append(text(" - "))
                    .append(alt.displayName().render())
                    .appendNewline();
            }
            user.sendMessage(component);
        }
    }
}
