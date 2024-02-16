package net.hollowcube.map.feature.experimental.object;

import com.google.auto.service.AutoService;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.worldold.MapWorld;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class TestObjectFeature implements FeatureProvider {

    @Override
    public void init(@NotNull ConfigLoaderV3 config) {
        MinecraftServer.getBlockManager().registerHandler(TestObjectBlock.INSTANCE.getNamespaceId(), () -> TestObjectBlock.INSTANCE);
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0 && false) {
            world.itemRegistry().register(TestObjectBlock.ITEM);
            return true;
        }

        return false;
    }
}
