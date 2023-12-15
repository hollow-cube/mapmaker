package net.hollowcube.mapmaker.to_be_refactored;

import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class ActionBar {

    private static final Tag<ActionBar> TAG = Tag.Transient("action_bar");

    public static @NotNull ActionBar forPlayer(@NotNull Player player) {
        var instance = player.getTag(TAG);
        if (instance == null) {
            instance = new ActionBar(player);
            player.setTag(TAG, instance);
        }
        return instance;
    }

    @FunctionalInterface
    public interface Provider {
        void provide(@NotNull Player player, @NotNull FontUIBuilder builder);

        default long expiration() {
            return -1;
        }
    }

    private final Set<Provider> providers = new HashSet<>();
    private final Player player;

    private ActionBar(@NotNull Player player) {
        this.player = player;

        player.scheduler().submitTask(this::update);
    }

    public void addProvider(@NotNull Provider provider) {
        providers.remove(provider); // Remove if already exists (to use latest version always)
        providers.add(provider);
    }

    public void removeProvider(@NotNull Provider provider) {
        providers.remove(provider);
    }

    private @NotNull TaskSchedule update() {
        //todo in the future, we should make this smarter to not recompute when values haven't changed.

        long now = System.currentTimeMillis();
        var builder = new FontUIBuilder();

        var iter = providers.iterator();
        while (iter.hasNext()) {
            var provider = iter.next();

            // Remove if expired
            if (provider.expiration() > 0 && provider.expiration() < now) {
                iter.remove();
                continue;
            }

            // Add it to action bar.
            var mark = builder.mark();
            provider.provide(player, builder);
            builder.restore(mark);
            builder.pos(0);
        }

        player.sendActionBar(builder.build(true));

        return TaskSchedule.tick(2);
    }

}
