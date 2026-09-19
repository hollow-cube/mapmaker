package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ChunkIndex;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.network.NetworkBuffer;

public record SpawnEntityEvent(int entityId, EntityType entityType, Pos position) implements ReplayEvent {
    public static final ReplayEventCodec<SpawnEntityEvent> CODEC = new ReplayEventCodec<>() {
        @Override
        public void write(NetworkBuffer buffer, SpawnEntityEvent event) {
            buffer.write(NetworkBuffer.VAR_INT, event.entityId());
            buffer.write(NetworkBuffer.STRING, event.entityType().key().asString());
            buffer.write(NetworkBuffer.POS, event.position());
        }

        @Override
        public SpawnEntityEvent read(NetworkBuffer buffer, ChunkIndex chunk) {
            var entityId = buffer.read(NetworkBuffer.VAR_INT);
            var entityType = ReplayGameData.readEntityType(buffer, chunk);
            return new SpawnEntityEvent(entityId, entityType, buffer.read(NetworkBuffer.POS));
        }
    };
}
