package net.hollowcube.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.feature.play.item.MapDetailsItem;
import net.hollowcube.map.feature.play.item.ReturnToHubItem;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.MapVariant;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class BaseBuildMapFeatureProvide implements FeatureProvider {

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/building", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld) || world.map().settings().getVariant() != MapVariant.BUILDING)
            return false;

        world.eventNode().addChild(eventNode);

        var itemRegistry = world.itemRegistry();
        itemRegistry.registerSilent(MapDetailsItem.INSTANCE);
        itemRegistry.registerSilent(ReturnToHubItem.INSTANCE);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!MapHooks.isPlayerPlaying(player)) return;

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
        inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));

        player.setAllowFlying(true);
    }

}
