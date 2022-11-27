package net.hollowcube.terraform.session;

import net.hollowcube.terraform.history.Change;
import net.hollowcube.terraform.region.selector.CuboidRegionSelector;
import net.hollowcube.terraform.region.selector.RegionSelector;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.tag.TagSerializer;
import net.minestom.server.tag.TagWritable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

/**
 * Global session data for a player, although we may choose to make some of this (like region selection, history) local to the world.
 */
public class Session {

    private static final Tag<Session> SESSION = Tag.Structure("terraform:session", new TagSerializer<Session>() {
        @Override
        public @Nullable Session read(@NotNull TagReadable reader) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void write(@NotNull TagWritable writer, @NotNull Session value) {
            throw new UnsupportedOperationException();
        }
    });

    public static @NotNull Session forPlayer(@NotNull Player player) {
        var session = player.getTag(SESSION);
        if (session == null) {
            session = new Session();
            player.setTag(SESSION, session);
        }

        return session;
    }

    private RegionSelector regionSelector = new CuboidRegionSelector();
    private final LinkedList<Change> history = new LinkedList<>();
    private int historyPointer = 0;

    public @NotNull RegionSelector getRegionSelector(@NotNull Instance instance) {
        if (!instance.equals(regionSelector.getInstance())) {
            regionSelector.clear();
            regionSelector.setInstance(instance);
        }

        return regionSelector;
    }

    // History

    public void remember(@NotNull Change change) {
        if (historyPointer != history.size())
            history.add(historyPointer, change);
        else history.add(change);
        historyPointer++;
    }

    public CompletableFuture<Void> undo(@NotNull Instance instance) {
        //todo need to decide what is instance local and what is tracking instances.

        if (historyPointer == 0)
            return CompletableFuture.failedFuture(new IllegalStateException("Nothing to undo"));
        historyPointer--;
        return history.get(historyPointer).undo(instance);
    }
}
