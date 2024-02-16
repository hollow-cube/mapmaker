package net.hollowcube.map.feature.experimental.pathtool;

import com.google.auto.service.AutoService;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.item.handler.ItemHandler;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class PathToolFeatureHandler implements FeatureProvider {

    private static final ItemHandler PATH_TOOL_ITEM = new PathToolItemHandler();

    public static final PathData thePath = new PathData("test_path");

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        // Only enabled in editing worlds.
        if (!(world instanceof EditingMapWorld) || true)
            return false;

        world.itemRegistry().register(PATH_TOOL_ITEM);

        Thread.startVirtualThread(() -> {
            //todo obviously this is stupid
            //thePath.addViewer(MinecraftServer.getConnectionManager().findPlayer("notmattw"));
        });

        var node = EventNode.type("abduawfaw", EventFilter.INSTANCE);
        node.addListener(InstanceTickEvent.class, event -> {
            thePath.tick();
        });
        world.eventNode().addChild(node);

        return true;
    }

}
