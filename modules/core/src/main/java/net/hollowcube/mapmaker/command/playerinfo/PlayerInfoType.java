package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface PlayerInfoType<T> {

    Argument<T> getArgument();

    void execute(@NotNull Player user, @NotNull T target);

    abstract class ForPlayer implements PlayerInfoType<Player> {

        @Override
        public Argument<Player> getArgument() {
            var argument = Argument.Entity("player").onlyPlayers(true).sameWorld(true);
            return argument.map((sender, raw) -> {
                var player = raw.findFirstPlayer(sender);
                if (player == null) {
                    return new ParseResult.Failure<>(0, "Player not found");
                }
                return new ParseResult.Success<>(player);
            });
        }

    }
}
