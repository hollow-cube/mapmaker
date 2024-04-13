package net.hollowcube.mapmaker.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.vanilla.DripleafBlock;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

@AutoService(FeatureProvider.class)
public class VanillaBlocksFeatureProvider implements FeatureProvider {

    @Override
    public @NotNull List<Supplier<BlockHandler>> blockHandlers() {
        return List.of(() -> DripleafBlock.INSTANCE);
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld))
            return false;

        world.eventNode().addListener(MapPlayerInitEvent.class, this::handlePlayerInit)
                .addListener(MapWorldPlayerStopPlayingEvent.class, this::handlePlayerDeinit);

        return true;
    }

    // Always reset the dripleaf state for the player when initializing.
    // This handles resetting on join (or any other potential weird state) because this event is
    // triggered when resetting a map.
    private void handlePlayerInit(MapPlayerInitEvent event) {
        DripleafBlock.INSTANCE.clearPlayer(event.getPlayer(), false);

        //todo i think the dripleaf list is ever expanding
    }

    private void handlePlayerDeinit(@NotNull MapWorldPlayerStopPlayingEvent event) {
        DripleafBlock.INSTANCE.clearPlayer(event.getPlayer(), true);
    }

}
