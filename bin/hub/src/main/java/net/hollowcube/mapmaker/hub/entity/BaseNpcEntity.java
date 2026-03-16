package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BaseNpcEntity extends Entity {

    public static BaseNpcEntity createInteractionEntity(int width, int height, NpcHandler handler) {
        var storeEntity = new BaseNpcEntity(EntityType.INTERACTION, UUID.randomUUID()) {
            @Override
            protected void movementTick() {
                // Intentionally do nothing
            }
        };
        storeEntity.setNoGravity(true);
        var interactionMeta = (InteractionMeta) storeEntity.getEntityMeta();
        interactionMeta.setWidth(width);
        interactionMeta.setHeight(height);
        storeEntity.setTag(NpcHandler.TAG, handler);
        return storeEntity;
    }

    private @Nullable Entity interactionEntity = null;

    public BaseNpcEntity(EntityType entityType, UUID uuid) {
        super(entityType, uuid);
    }

    public void setHandler(NpcHandler handler) {
        this.setTag(NpcHandler.TAG, handler);
    }

    public CompletableFuture<Void> setInteractionBox(int width, int height) {
        return setInteractionBox(width, height, Pos.ZERO);
    }

    public CompletableFuture<Void> setInteractionBox(int width, int height, Pos offset) { //todo move along with the npc
        if (this.interactionEntity != null) this.interactionEntity.remove();

        this.interactionEntity = new BaseNpcEntity(EntityType.INTERACTION, UUID.randomUUID()) {
            @Override
            protected void movementTick() {
                // Intentionally do nothing
            }
        };
        this.interactionEntity.setNoGravity(true);
        var interactionMeta = (InteractionMeta) interactionEntity.getEntityMeta();
        interactionMeta.setWidth(width);
        interactionMeta.setHeight(height);
        interactionMeta.setResponse(true);

        this.interactionEntity.setTag(NpcHandler.TAG, (player, npc, hand, isLeftHand) -> {
            if (!BaseNpcEntity.this.hasTag(NpcHandler.TAG)) return;
            BaseNpcEntity.this.getTag(NpcHandler.TAG).handlePlayerInteract(player, BaseNpcEntity.this, hand, isLeftHand);
        });
        return this.interactionEntity.setInstance(this.getInstance(), this.getPosition().add(offset));
    }
}
