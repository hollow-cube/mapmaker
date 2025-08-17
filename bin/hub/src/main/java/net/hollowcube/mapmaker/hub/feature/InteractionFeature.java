package net.hollowcube.mapmaker.hub.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.common.physics.RayUtils2;
import net.hollowcube.common.physics.SweepResult2;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.util.InteractionEntity;
import net.hollowcube.mapmaker.map.MapServer;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
public class InteractionFeature implements HubFeature {
    private static final Tag<InteractionEntity> LAST_ENTITY = Tag.Transient("last_hovered_entity");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("interaction-event", EventFilter.INSTANCE)
            .addListener(InstanceTickEvent.class, this::onTick)
            .addListener(PlayerEntityInteractEvent.class, this::handleEntityInteract);
    private HubMapWorld world;

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        this.world = world;
        world.eventNode().addChild(eventNode);
    }

    private void onTick(@NotNull InstanceTickEvent event) {
        for (var player : world.players()) {
            var rayStart = player.getPosition().add(0, player.getEyeHeight(), 0);
            var rayDirection = rayStart.direction();

            InteractionEntity hitEntity = null;
            boolean wasInside = false;
            var result = new SweepResult2();
            for (var e : world.instance().getEntities()) {
                if (!(e instanceof InteractionEntity entity)) continue;

                final BoundingBox entityBB = entity.getBoundingBox();

                if (RayUtils2.BoundingBoxIntersectionCheck(rayStart, rayDirection, entityBB, entity.getPosition(), result)) {
                    hitEntity = entity;
                } else if (RayUtils2.boundingBoxContainsPoint(entityBB, entity.getPosition(), rayStart)) {
                    hitEntity = entity;
                    wasInside = true;
                    break; // If we are inside the entity its always a hit
                }
            }

            // Now ensure we arent looking at a block before the entity, and that the entity is closer than the target interaction distance.
            if (!wasInside && hitEntity != null) {
                double distance = rayStart.distance(result.collidedPositionX(), result.collidedPositionY(), result.collidedPositionZ());
                if (hitEntity.interactionDistance() < distance) hitEntity = null;
                else if (PlayerUtil.getTargetBlock(player, distance, true) != null) hitEntity = null;
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

    private void handleEntityInteract(PlayerEntityInteractEvent event) {
        if (!(event.getTarget() instanceof InteractionEntity entity)) return;
        var player = event.getPlayer();

        // Do our own sweep to check interaction distance (with a little leniency for ping).
        // A check from the interaction entity to the player position will be significantly different than the test
        // we do during the above hover check, so do this for better accuracy.
        var result = new SweepResult2();
        var rayStart = player.getPosition().add(0, player.getEyeHeight(), 0);
        boolean hit = RayUtils2.BoundingBoxIntersectionCheck(rayStart, rayStart.direction(),
                entity.getBoundingBox(), entity.getPosition(), result);
        if (!hit || result.getCollidedPosition().distance(rayStart) > entity.interactionDistance() + 0.5)
            return;
        // Only trigger the right click if they dont have another item or they are sneaking.
        if (!player.isSneaking() && !player.getItemInMainHand().isAir())
            return;

        entity.target().onRightClick(player);
    }
}
