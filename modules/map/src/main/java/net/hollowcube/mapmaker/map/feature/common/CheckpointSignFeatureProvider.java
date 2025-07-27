package net.hollowcube.mapmaker.map.feature.common;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.handler.SignBlockHandler;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.object.ObjectBlockHandler;
import net.hollowcube.mapmaker.map.object.ObjectTypes;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockInteractEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

/// Implements handling for editing and using checkpoint signs.
/// This is not done as a BlockHandler because SignBlockHandler currently has too much inflexible behavior.
///
/// In the future maybe we should refactor it to be a bit more flexible and then use a block handler, but until
/// then this is OK.
@AutoService(FeatureProvider.class)
public class CheckpointSignFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> editEventNode = EventNode.type("checkpoint-sign-edit", EventFilter.INSTANCE)
            .addListener(PlayerBlockPlaceEvent.class, this::handleSignPlace)
            .addListener(PlayerBlockInteractEvent.class, this::handleEditingSignClick);

    private final EventNode<InstanceEvent> playEventNode = EventNode.type("checkpoint-sign-play", EventFilter.INSTANCE)
            .addListener(PlayerBlockInteractEvent.class, this::handlePlayingSignClick);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (world instanceof EditingMapWorld) {
            world.eventNode().addChild(editEventNode);
            return true;
        } else if (world instanceof PlayingMapWorld || world instanceof TestingMapWorld) {
            world.eventNode().addChild(playEventNode);
            return true;
        } else return false;
    }

    private void handleSignPlace(@NotNull PlayerBlockPlaceEvent event) {
//        var player = event.getPlayer();
//        var world = MapWorld.forPlayerOptional(player);
//        if (world == null || !world.canEdit(player)) return; // Sanity
//
//        event.setBlock(event.getBlock().withNbt(event.getBlock().nbtOrEmpty().put("checkpoint", CompoundBinaryTag.empty())));
    }

    private void handleEditingSignClick(@NotNull PlayerBlockInteractEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.canEdit(player) || !player.isSneaking())
            return; // Sanity

        var block = event.getBlock();
        if (!(block.handler() instanceof SignBlockHandler) || event.getHand() != PlayerHand.MAIN)
            return;

        var checkpoint = block.getTag(CheckpointPlateBlock.SIGN_DATA_TAG);
        if (checkpoint == null) return;

        // Open checkpoint settings GUI
        var maxResetHeight = event.getBlockPosition().blockY();
//        world.server().showView(player, c -> new EditCheckpointView(c.with(Map.of("updateTarget", event.getBlockPosition())), checkpoint, maxResetHeight, () -> {
//            var instance = event.getInstance();
//            var blockPosition = event.getBlockPosition();
//
//            instance.setBlock(blockPosition, event.getBlock().withTag(CheckpointPlateBlock.SIGN_DATA_TAG, checkpoint));
//        }));
    }

    private void handlePlayingSignClick(@NotNull PlayerBlockInteractEvent event) {
        if (true) return;

        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null || !world.isPlaying(player)) return; // Sanity

        var block = event.getBlock();
        if (!(block.handler() instanceof SignBlockHandler) || event.getHand() != PlayerHand.MAIN)
            return;

        var checkpoint = block.getTag(CheckpointPlateBlock.SIGN_DATA_TAG);
        if (checkpoint == null) return;

        event.setCancelled(true);
        var checkpointId = ObjectBlockHandler.createObjectId(ObjectTypes.CHECKPOINT_PLATE, event.getBlockPosition());
//        world.callEvent(new MapPlayerCheckpointPreChangeEvent(player, world, checkpointId, checkpoint));
    }
}
