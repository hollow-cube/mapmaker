package net.hollowcube.map.block;

import com.google.auto.service.AutoService;
import net.hollowcube.common.facet.Facet;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.map.event.MapWorldCheckpointReachedEvent;
import net.hollowcube.map.event.MapWorldRegisterEvent;
import net.hollowcube.map.event.MapWorldUnregisterEvent;
import net.hollowcube.map.item.ItemManager;
import net.hollowcube.map.item.NamedItems;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.ServerProcess;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

@AutoService(Facet.class)
public class CheckpointPlateBlock implements Facet {
    public static final NamespaceID ID = NamespaceID.from("mapmaker:checkpoint_plate");
    public static final String POI_TYPE = "mapmaker:checkpoint_plate";

    public static final ItemStack ITEM = ItemStack.builder(Material.HEAVY_WEIGHTED_PRESSURE_PLATE)
            .meta(m -> m.customModelData(NamedItems.CHECKPOINT_PLATE))
            .displayName(Component.translatable("item.checkpoint_plate.name"))
            .lore(LanguageProvider.createMultiTranslatable("item.checkpoint_plate.lore"))
            .build();

    private static final EventNode<? extends InstanceEvent> eventNode = EventNode.type("mapmaker:item/checkpoint_plate", EventFilter.INSTANCE)
            .setPriority(-1000)
            .addListener(EventListener.builder(PlayerBlockPlaceEvent.class)
                    .filter(event -> isCheckpointPlate(event.getPlayer().getItemInHand(event.getHand())))
                    .ignoreCancelled(true)
                    .handler(CheckpointPlateBlock::handlePlacement)
                    .build());

    @Override
    public void hook(@NotNull ServerProcess server) {
        ItemManager.register(ID, ITEM);
        server.block().registerHandler(Handler.INSTANCE.getNamespaceId(), () -> Handler.INSTANCE);
        server.eventHandler().addListener(MapWorldRegisterEvent.class, event -> event.getInstance()
                .eventNode().addChild(eventNode));
        server.eventHandler().addListener(MapWorldUnregisterEvent.class, event -> event.getInstance()
                .eventNode().removeChild(eventNode));
    }

    public static void handlePlacement(@NotNull PlayerBlockPlaceEvent event) {
        var map = MapWorld.fromInstance(event.getInstance()).map();
        map.addPOI(new MapData.POI(POI_TYPE, event.getBlockPosition()));
        event.setBlock(event.getBlock().withHandler(Handler.INSTANCE));
    }

    private static boolean isCheckpointPlate(@NotNull ItemStack itemStack) {
        return itemStack.meta().getCustomModelData() == NamedItems.CHECKPOINT_PLATE;
    }

    public static class Handler extends AbstractPlateHandler {
        public static final Handler INSTANCE = new Handler();

        @Override
        public @NotNull NamespaceID getNamespaceId() {
            return ID;
        }

        @Override
        public void onPlatePressed(@NotNull Tick tick, @NotNull Player player) {
            var instance = tick.getInstance();
            EventDispatcher.call(new MapWorldCheckpointReachedEvent(MapWorld.fromInstance(instance), player));
        }

        @Override
        public void onDestroy(@NotNull Destroy destroy) {
            var map = MapWorld.fromInstance(destroy.getInstance()).map();
            map.removePOI(destroy.getBlockPosition());
        }
    }
}
