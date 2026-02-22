package net.hollowcube.mapmaker.to_be_refactored;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FutureUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;

public final class ActionBar {
    //todo can just fold this into MapPlayer2.

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

        default int cacheKey(@NotNull Player player) {
            return ThreadLocalRandom.current().nextInt();
        }
    }

    private final Set<Provider> providers = new CopyOnWriteArraySet<>(); // todo later can make this not copy
    private final Player player;

    private Component lastContent = Component.empty();
    private int lastHash = 0;

    private ActionBar(@NotNull Player player) {
        this.player = player;

        player.scheduler().submitTask(this::update);
    }

    public void addProvider(@NotNull Provider provider) {
        FutureUtil.assertTickThreadWarn();
        providers.remove(provider); // Remove if already exists (to use latest version always)
        providers.add(provider);
    }

    public void removeProvider(@NotNull Provider provider) {
        FutureUtil.assertTickThreadWarn();
        providers.remove(provider);
    }

    public void toggleProvider(@NotNull Provider provider) {
        FutureUtil.assertTickThreadWarn();
        if (!providers.remove(provider)) {
            providers.add(provider);
        }
    }

    private @NotNull TaskSchedule update() {
        if (player.getPlayerConnection().getConnectionState() != ConnectionState.PLAY)
            return TaskSchedule.tick(2);

        long now = System.currentTimeMillis();
        providers.removeIf(provider -> provider.expiration() > 0 && provider.expiration() < now);

        int hash = 1;
        for (Provider provider : providers) {
            hash = 31 * hash + provider.cacheKey(player);
            hash = 31 * hash + provider.getClass().hashCode();
        }

        if (hash != lastHash) {
            lastHash = hash;

            var builder = new FontUIBuilder();
            for (Provider provider : providers) {
                // Add it to action bar.
                var mark = builder.mark();
                provider.provide(player, builder);
                builder.restore(mark);
                builder.pos(0);
            }

            lastContent = builder.build(true);
        }

        player.sendActionBar(lastContent);
        return TaskSchedule.tick(2);
    }

}
