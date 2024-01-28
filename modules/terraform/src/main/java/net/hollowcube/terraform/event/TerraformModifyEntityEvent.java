package net.hollowcube.terraform.event;

import net.minestom.server.entity.Entity;
import net.minestom.server.event.trait.EntityInstanceEvent;
import org.jetbrains.annotations.NotNull;

public class TerraformModifyEntityEvent implements EntityInstanceEvent {
    private final Entity entity;

    public TerraformModifyEntityEvent(Entity entity) {
        this.entity = entity;
    }

    @Override
    public @NotNull Entity getEntity() {
        return entity;
    }
}
