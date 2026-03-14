package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;

import java.util.UUID;

public class NpcTextModel extends BaseNpcEntity {

    public NpcTextModel() {
        this(UUID.randomUUID());
    }

    public NpcTextModel(UUID uuid) {
        super(EntityType.TEXT_DISPLAY, uuid);

        hasPhysics = false;
        setNoGravity(true);
    }

    @Override
    protected void movementTick() {
        // Intentionally do nothing
    }

    @Override
    public TextDisplayMeta getEntityMeta() {
        return (TextDisplayMeta) super.getEntityMeta();
    }
}
