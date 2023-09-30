package net.hollowcube.map.feature.experimental.object;

import com.google.auto.service.AutoService;
import net.hollowcube.common.config.ConfigProvider;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class TestObjectFeature implements FeatureProvider {

    @Override
    public void init(@NotNull ConfigProvider config) {
        MinecraftServer.getBlockManager().registerHandler(TestObjectBlock.INSTANCE.getNamespaceId(), () -> TestObjectBlock.INSTANCE);
    }

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if ((world.flags() & MapWorld.FLAG_EDITING) != 0) {
            world.itemRegistry().register(TestObjectBlock.ITEM);
            return true;
        }

        return false;
    }
}
