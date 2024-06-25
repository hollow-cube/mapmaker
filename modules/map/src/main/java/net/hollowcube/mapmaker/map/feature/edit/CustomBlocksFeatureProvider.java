package net.hollowcube.mapmaker.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.custom.BouncePadBlock;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.FinishPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.feature.edit.item.BuilderMenuItem;
import net.hollowcube.mapmaker.map.feature.edit.item.EnterTestModeItem;
import net.hollowcube.mapmaker.map.feature.edit.item.SpawnPointItem;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

@AutoService(FeatureProvider.class)
public class CustomBlocksFeatureProvider implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("custom-blocks-event-node", EventFilter.INSTANCE)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::cleanupPlayer);

    @Override
    public @NotNull List<Supplier<BlockHandler>> blockHandlers() {
        return List.of(
                FinishPlateBlock::new,
                CheckpointPlateBlock::new,
                BouncePadBlock::new,
                StatusPlateBlock::new
        );
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (world instanceof EditingMapWorld) {
            world.itemRegistry().register(FinishPlateBlock.ITEM);
            world.itemRegistry().register(CheckpointPlateBlock.ITEM);
            world.itemRegistry().register(BouncePadBlock.ITEM);
            world.itemRegistry().register(StatusPlateBlock.ITEM);

            //todo this shouldnt be registered from here
            world.itemRegistry().register(BuilderMenuItem.INSTANCE);
            world.itemRegistry().register(EnterTestModeItem.INSTANCE);
            world.itemRegistry().register(SpawnPointItem.INSTANCE);

            world.eventNode().addChild(eventNode);

            return true;
        }

        return false;
    }

    private void cleanupPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();
        player.removeTag(BaseEffectData.TARGET_PLATE);
    }

}
