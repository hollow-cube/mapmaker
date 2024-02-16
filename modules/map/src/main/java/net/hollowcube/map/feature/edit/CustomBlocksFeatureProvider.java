package net.hollowcube.map.feature.edit;

import com.google.auto.service.AutoService;
import net.hollowcube.map.block.custom.BouncePadBlock;
import net.hollowcube.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.map.block.custom.FinishPlateBlock;
import net.hollowcube.map.block.custom.StatusPlateBlock;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.feature.edit.item.BuilderMenuItem;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map2.MapWorld;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(FeatureProvider.class)
public class CustomBlocksFeatureProvider implements FeatureProvider {

    @Override
    public @NotNull List<BlockHandler> blockHandlers() {
        return List.of(FinishPlateBlock.INSTANCE, CheckpointPlateBlock.INSTANCE, BouncePadBlock.INSTANCE, StatusPlateBlock.INSTANCE);
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
            return true;
        }

        return false;
    }

}
