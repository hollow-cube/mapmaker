package net.hollowcube.mapmaker.map.entity.interaction;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntity;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class InteractionEntity extends ObjectEntity<InteractionMeta> {

    public InteractionEntity(@NotNull UUID uuid) {
        super(EntityType.INTERACTION, uuid);

        this.sendToClient = true;

        this.getEntityMeta().setResponse(true);
    }

    public InteractionEntity() {
        this(UUID.randomUUID());
    }

    @Override
    public void setBoundingBox(BoundingBox boundingBox) {
        super.setBoundingBox(boundingBox);

        float width = (float) Math.max(boundingBox.width(), boundingBox.depth());
        float height = (float) boundingBox.height();

        this.getEntityMeta().setWidth(width);
        this.getEntityMeta().setHeight(height);
        this.setTag(REGION_MIN_TAG, new Vec(-width / 2, 0, -width / 2));
        this.setTag(REGION_MAX_TAG, new Vec(width / 2, height, width / 2));
    }

    @Override
    public void onRightClick(@NotNull MapWorld world, @NotNull Player player, @NotNull PlayerHand hand, @NotNull Point interactPosition) {
        if (world.canEdit(player)) {
            InteractionEditorScreen.openEditorScreen(this, player);
        } else if (this.handler != null) {
            this.handler.onPlayerInteract(player);
        }
    }
}
