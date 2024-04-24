package net.hollowcube.mapmaker.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapChangeSpawnPointEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.feature.play.BaseParkourMapFeatureProvider;
import net.hollowcube.mapmaker.map.gui.effect.EditCheckpointView;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.util.CoreTeams;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.server.network.packet.server.play.TeamsPacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
@AutoService(FeatureProvider.class)
public class MapSpawnIndicatorFeatureProvider implements FeatureProvider {

    private static final Tag<SpawnMarkerEntity> SPAWN_MARKER_TAG = Tag.Transient("map:spawn_marker");

    private final EventNode<InstanceEvent> eventNode = EventNode.type("map:spawn_indicator", EventFilter.INSTANCE)
            .addListener(MapChangeSpawnPointEvent.class, this::handleSpawnPointChange)
            .addListener(PlayerEntityInteractEvent.class, this::handleEntityInteract);

    private final EventNode<InstanceEvent> testEventNode = EventNode.type("map:spawn_indicator_test", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::handleSpawnInTestMode);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (world instanceof TestingMapWorld) {
            world.eventNode().addChild(testEventNode);
            return true;
        } else if (!(world instanceof EditingMapWorld)) {
            return false;
        }

        world.eventNode().addChild(eventNode);

        var spawnMarker = new SpawnMarkerEntity();
        spawnMarker.setInstance(world.instance(), world.map().settings().getSpawnPoint());
        world.setTag(SPAWN_MARKER_TAG, spawnMarker);

        return true;
    }

    private void handleSpawnPointChange(@NotNull MapChangeSpawnPointEvent event) {
        var world = event.getMapWorld();
        var spawnMarker = world.getTag(SPAWN_MARKER_TAG);
        if (spawnMarker == null) return;

        spawnMarker.teleport(event.newSpawnPoint());
    }

    private void handleEntityInteract(@NotNull PlayerEntityInteractEvent event) {
        if (!(event.getTarget() instanceof SpawnMarkerEntity entity))
            return;

        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player)) return;

        if (world.map().settings().getVariant() != MapVariant.PARKOUR) {
            player.sendMessage("can only edit spawn settings on parkour maps.");
            return;
        }

        // Open checkpoint settings view
        var controller = world.server().guiController();
        int maxResetHeight = entity.getPosition().blockY();
        var checkpointData = world.getTag(BaseParkourMapFeatureProvider.SPAWN_CHECKPOINT_EFFECTS);
        controller.show(player, c -> new EditCheckpointView(c, checkpointData, maxResetHeight,
                () -> world.setTag(BaseParkourMapFeatureProvider.SPAWN_CHECKPOINT_EFFECTS, checkpointData)));
    }

    private void handleSpawnInTestMode(@NotNull MapPlayerInitEvent event) {
        var world = event.getMapWorld();
        var spawnMarker = world.getTag(SPAWN_MARKER_TAG);
        if (spawnMarker != null) spawnMarker.updateViewableRule();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static class SpawnMarkerEntity extends Entity {

        public SpawnMarkerEntity() {
            super(EntityType.PLAYER, UUID.randomUUID());

            setNoGravity(true);
            hasPhysics = false;
            hasCollision = false;

            // Only show the entity to players in build mode
            updateViewableRule(p -> MapWorld.forPlayerOptional(p) instanceof EditingMapWorld world && world.canEdit(p));
        }

        @Override
        public void updateNewViewer(@NotNull Player player) {
            var properties = new ArrayList<PlayerInfoUpdatePacket.Property>();
            var viewerSkin = player.getSkin();
            if (viewerSkin != null) {
                properties.add(new PlayerInfoUpdatePacket.Property("textures", viewerSkin.textures(), viewerSkin.signature()));
            }

            var entry = new PlayerInfoUpdatePacket.Entry(getUuid(), "Spawn Point", properties, false,
                    0, GameMode.SURVIVAL, null, null);
            player.sendPacket(new PlayerInfoUpdatePacket(PlayerInfoUpdatePacket.Action.ADD_PLAYER, entry));

            // Spawn the player entity
            super.updateNewViewer(player);

            player.sendPacket(new TeamsPacket(CoreTeams.DEFAULT.getTeamName(), new TeamsPacket.AddEntitiesToTeamAction(List.of("Spawn Point"))));

            setInvisible(true);
        }

        @Override
        public void updateOldViewer(@NotNull Player player) {
            super.updateOldViewer(player);

            player.sendPacket(new PlayerInfoRemovePacket(getUuid()));
        }
    }
}
