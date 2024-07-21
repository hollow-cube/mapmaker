package net.hollowcube.mapmaker.hub.feature;

import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import net.hollowcube.common.physics.RayUtils2;
import net.hollowcube.common.physics.SweepResult2;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.util.InteractionEntity;
import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
public class InteractionFeature implements HubFeature {
    private static final Tag<InteractionEntity> LAST_ENTITY = Tag.Transient("last_hovered_entity");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("interaction-event", EventFilter.INSTANCE)
            .addListener(InstanceTickEvent.class, this::onTick);
    private final HubMapWorld world;

    @Inject
    public InteractionFeature(@NotNull HubMapWorld world) {
        this.world = world;

        world.eventNode().addChild(eventNode);
    }

    private void onTick(@NotNull InstanceTickEvent event) {
        for (var player : world.players()) {
            var rayStart = player.getPosition().add(0, player.getEyeHeight(), 0);
            var rayDirection = rayStart.direction();

            InteractionEntity hitEntity = null;
            var result = new SweepResult2();
            for (var e : world.instance().getEntities()) {
                if (!(e instanceof InteractionEntity entity)) continue;

                final BoundingBox entityBB = entity.getBoundingBox();
                if (RayUtils2.BoundingBoxIntersectionCheck(rayStart, rayDirection, entityBB, entity.getPosition(), result)) {
                    hitEntity = entity;
                } else if (RayUtils2.boundingBoxContainsPoint(entityBB, entity.getPosition(), rayStart)) {
                    hitEntity = entity;
                    break; // If we are inside the entity its always a hit
                }
            }

            // Now ensure we arent looking at a block before the entity
            if (hitEntity != null) {
                double distance = rayStart.distance(result.collidedPositionX(), result.collidedPositionY(), result.collidedPositionZ());
                if (PlayerUtil.getTargetBlock(player, distance) != null) hitEntity = null;
            }

            var lastEntity = player.getTag(LAST_ENTITY);
            if (lastEntity == hitEntity) continue;
            if (lastEntity != null) lastEntity.target().endHover(player);
            if (hitEntity != null) {
                hitEntity.target().beginHover(player);
                player.setTag(LAST_ENTITY, hitEntity);
            } else {
                player.removeTag(LAST_ENTITY);
            }
        }
    }
}
