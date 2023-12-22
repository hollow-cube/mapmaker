package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BaseNpcEntity extends Entity {
    private Entity interactionEntity = null;

    public BaseNpcEntity(@NotNull EntityType entityType, @NotNull UUID uuid) {
        super(entityType, uuid);
    }

    public void setHandler(@NotNull NpcHandler handler) {
        this.setTag(NpcHandler.TAG, handler);
    }

    public void setInteractionBox(int width, int height) { //todo move along with the npc
        if (this.interactionEntity != null) this.interactionEntity.remove();

        this.interactionEntity = new BaseNpcEntity(EntityType.INTERACTION, UUID.randomUUID());
        var interactionMeta = (InteractionMeta) interactionEntity.getEntityMeta();
        interactionMeta.setWidth(width);
        interactionMeta.setHeight(height);

        this.interactionEntity.setTag(NpcHandler.TAG, (player, npc, hand) -> {
            if (!BaseNpcEntity.this.hasTag(NpcHandler.TAG)) return;
            BaseNpcEntity.this.getTag(NpcHandler.TAG).handlePlayerInteract(player, BaseNpcEntity.this, hand);
        });
        this.interactionEntity.setInstance(this.getInstance(), this.getPosition());
    }
}
