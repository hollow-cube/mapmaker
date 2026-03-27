package net.hollowcube.mapmaker.map;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

public record PlayerStateChange<S extends PlayerState<@NotNull S, @NotNull W>, W extends AbstractMapWorld<@NotNull S, @NotNull W>>(
        S state,
        BiPredicate<@NotNull Player, @NotNull S> predicate
) {

    public boolean canChange(@NotNull Player player) {
        return this.predicate.test(player, this.state);
    }

    public PlayerStateChange<S, W> handleConflict(PlayerStateChange<S, W> other) {
        var state = this.state.handleConflict(other.state());
        return state == this.state ? this : other;
    }
}
