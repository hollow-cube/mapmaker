package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

class AltsInfoType implements PlayerInfoType<String> {

    private final PlayerClient players;

    public AltsInfoType(@NotNull PlayerClient players) {
        this.players = players;
    }

    @Override
    public Argument<String> getArgument() {
        return CoreArgument.AnyPlayerId("player", players);
    }

    @Override
    public void execute(@NotNull Player user, @NotNull String target) {
        var alts = players.getAlts(target);
        if (alts.isEmpty()) {
            user.sendMessage("No alts found for %s".formatted(target));
        } else {
            var component = text()
                .append(text("Alts for "))
                .append(players.getDisplayName(target).build())
                .append(text(":"))
                .appendNewline();
            for (var alt : alts) {
                component = component.append(text(" - "))
                    .append(alt.displayName())
                    .appendNewline();
            }
            user.sendMessage(component);
        }
    }
}
