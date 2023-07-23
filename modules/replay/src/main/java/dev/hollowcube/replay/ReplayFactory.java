package dev.hollowcube.replay;

import dev.hollowcube.replay.change.RecordedPlayerMove;
import dev.hollowcube.replay.change.RecordedPlayerSpawn;
import dev.hollowcube.replay.change.ReplayChangeId;
import dev.hollowcube.replay.event.InstanceEndTickEvent;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import org.jetbrains.annotations.NotNull;

public class ReplayFactory {

    static {
        MinecraftServer.getGlobalEventHandler().addChild(EventNode.all("replay/end-tick")
                .setPriority(-10000000)
                .addListener(InstanceTickEvent.class, event -> EventDispatcher.call(new InstanceEndTickEvent(event.getInstance()))));
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    private final Int2ObjectMap<RecordedChange.Reader> readers;

    public ReplayFactory(@NotNull Int2ObjectMap<RecordedChange.Reader> readers) {
        this.readers = readers;
    }


    public static class Builder {
        private final Int2ObjectMap<RecordedChange.Reader> readers = new Int2ObjectArrayMap<>();

        Builder() {
            register(ReplayChangeId.PLAYER_SPAWN, RecordedPlayerSpawn::new);
            register(ReplayChangeId.PLAYER_POSITION, RecordedPlayerMove::new);
        }

        public @NotNull Builder register(int id, @NotNull RecordedChange.Reader reader) {
            readers.put(id, reader);
            return this;
        }

        public @NotNull ReplayFactory build() {
            return new ReplayFactory(readers);
        }
    }

}
