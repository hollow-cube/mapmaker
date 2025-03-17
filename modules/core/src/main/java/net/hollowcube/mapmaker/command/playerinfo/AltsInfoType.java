package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

class AltsInfoType implements PlayerInfoType<String> {

    private final PlayerService players;

    public AltsInfoType(PlayerService players) {
        this.players = players;
    }

    @Override
    public Argument<String> getArgument() {
        return CoreArgument.AnyPlayerId("player", this.players);
    }

    @Override
    public void execute(@NotNull Player user, @NotNull String target) {
        var alts = this.players.getAlts(target);
        if (alts.isEmpty()) {
            user.sendMessage("No alts found for %s".formatted(target));
        } else {
            Component component = Component.text("Alts for ")
                    .append(this.players.getPlayerDisplayName2(target).build())
                    .append(Component.text(":"))
                    .appendNewline();
            for (var alt : alts) {
                component = component.append(Component.text(" - %s".formatted(alt.username()))).appendNewline();
            }
            user.sendMessage(component);
        }
    }
}
