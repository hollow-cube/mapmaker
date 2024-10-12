package net.hollowcube.mapmaker.map.feature.play;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.map.MapFeatureFlags;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.MapPlayerInitEvent;
import net.hollowcube.mapmaker.map.event.MapWorldPlayerStopPlayingEvent;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.feature.play.item.MapDetailsItem;
import net.hollowcube.mapmaker.map.feature.play.item.RateMapItem;
import net.hollowcube.mapmaker.map.feature.play.item.ReturnToHubItem;
import net.hollowcube.mapmaker.map.util.CustomizableHotbarManager;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@AutoService(FeatureProvider.class)
public class BaseBuildMapFeatureProvide implements FeatureProvider {

    private static final CustomizableHotbarManager BUILDING_HOTBAR = CustomizableHotbarManager.builder("hotbar/build")
            .defaultItem(0, MapDetailsItem.ID)
            .defaultItem(2, RateMapItem.ID, (_, world) -> MapRatingFeatureProvider.isMapRatable(world))
            .defaultItem(8, ReturnToHubItem.ID)
            .build();

    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:play/building", EventFilter.INSTANCE)
            .addListener(MapPlayerInitEvent.class, this::initPlayer)
            .addListener(MapWorldPlayerStopPlayingEvent.class, this::deinitPlayer)
            .addListener(PlayerTickEvent.class, this::handlePlayerTick);

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld) || world.map().settings().getVariant() != MapVariant.BUILDING)
            return false;

        BUILDING_HOTBAR.registerEvents(world.eventNode());
        world.eventNode().addChild(eventNode);

        var itemRegistry = world.itemRegistry();
        itemRegistry.registerSilent(CustomizableHotbarManager.RESET_TO_DEFAULT_ITEM);
        itemRegistry.registerSilent(MapDetailsItem.INSTANCE);
        itemRegistry.registerSilent(ReturnToHubItem.INSTANCE);
        itemRegistry.registerSilent(RateMapItem.INSTANCE);

        return true;
    }

    public void initPlayer(@NotNull MapPlayerInitEvent event) {
        var player = event.getPlayer();
        if (!event.getMapWorld().isPlaying(player)) return;

        // Set the hotbar
        var itemRegistry = event.mapWorld().itemRegistry();
        var inventory = player.getInventory();
        if (MapFeatureFlags.CUSTOMIZABLE_HOTBAR.test(player)) {
            BUILDING_HOTBAR.apply(player, event.mapWorld());
        } else {
            inventory.setItemStack(0, itemRegistry.getItemStack(MapDetailsItem.ID, null));
            inventory.setItemStack(2, itemRegistry.getItemStack(RateMapItem.ID, null));
            inventory.setItemStack(8, itemRegistry.getItemStack(ReturnToHubItem.ID, null));
        }

        player.setAllowFlying(true);
    }

    public void deinitPlayer(@NotNull MapWorldPlayerStopPlayingEvent event) {
        var player = event.getPlayer();
        CustomizableHotbarManager.unregister(player);
    }

    public void handlePlayerTick(@NotNull PlayerTickEvent event) {
        var player = event.getPlayer();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        if (player.getPosition().y() < world.instance().getCachedDimensionType().minY()) {
            player.teleport(world.spawnPoint(player));
        }
    }

}
