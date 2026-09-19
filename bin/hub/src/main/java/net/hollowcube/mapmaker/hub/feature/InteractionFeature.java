package net.hollowcube.mapmaker.hub.feature;

import com.google.auto.service.AutoService;
import net.hollowcube.common.physics.Shapes;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.util.InteractionEntity;
import net.hollowcube.mapmaker.map.MapServer;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
            double hitDistance = Double.MAX_VALUE;
            for (var e : world.instance().getEntities()) {
                if (!(e instanceof InteractionEntity entity)) continue;

                var box = Shapes.absolute(entity.getPosition(), entity.getBoundingBox());
                if (Shapes.containsPoint(box, rayStart)) {
                    hitEntity = entity;
                    wasInside = true;
                    break; // If we are inside the entity its always a hit
                }

                var reach = rayStart.add(rayDirection.mul(entity.interactionDistance()));
                var hit = Shapes.clip(List.of(box), rayStart, reach);
                if (hit == null) continue;

                double distance = rayStart.distance(hit.position());
                if (distance >= hitDistance) continue;
                hitDistance = distance;
                hitEntity = entity;
            }

            // Now ensure we arent looking at a block before the entity.
            if (!wasInside && hitEntity != null && PlayerUtil.getTargetBlock(player, hitDistance, true) != null)
                hitEntity = null;

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
        var rayStart = player.getPosition().add(0, player.getEyeHeight(), 0);
        var reach = rayStart.add(rayStart.direction().mul(entity.interactionDistance() + 0.5));
        var box = Shapes.absolute(entity.getPosition(), entity.getBoundingBox());
        if (Shapes.clip(List.of(box), rayStart, reach) == null) return;
        // Only trigger the right click if they dont have another item or they are sneaking.
        if (!player.isSneaking() && !player.getItemInMainHand().isAir())
            return;

        entity.target().onRightClick(player);
    }
}
