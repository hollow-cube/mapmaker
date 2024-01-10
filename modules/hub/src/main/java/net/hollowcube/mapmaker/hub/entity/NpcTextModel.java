package net.hollowcube.mapmaker.hub.entity;

import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NpcTextModel extends BaseNpcEntity {

    public NpcTextModel() {
        this(UUID.randomUUID());
    }

    public NpcTextModel(@NotNull UUID uuid) {
        super(EntityType.TEXT_DISPLAY, uuid);

        hasPhysics = false;
        setNoGravity(true);
    }

    @Override
    public @NotNull TextDisplayMeta getEntityMeta() {
        return (TextDisplayMeta) super.getEntityMeta();
    }
}
