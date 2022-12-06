package net.hollowcube.map.block;

import com.google.auto.service.AutoService;
import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapWorldCompleteEvent;
import net.hollowcube.map.event.MapWorldRegisterEvent;
import net.hollowcube.map.item.FinishPlateItem;
import net.hollowcube.map.item.ItemManager;
import net.hollowcube.map.item.NamedItems;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.facet.Facet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.ServerProcess;
import net.minestom.server.collision.BoundingBox;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.*;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

@AutoService(Facet.class)
public class FinishPlateBlock implements Facet {
    public static final NamespaceID ID = NamespaceID.from("mapmaker:finish_plate");

    public static final ItemStack ITEM = ItemStack.builder(Material.LIGHT_WEIGHTED_PRESSURE_PLATE)
            .meta(m -> m.customModelData(NamedItems.FINISH_PLATE))
            .displayName(Component.text("Finish Plate", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false))
            .lore(
                    Component.text("Marks the finish line of the map.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("You can place multiple of these at once.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ).build();

    private static EventNode<? extends InstanceEvent> node = EventNode.type("mapmaker:item/finish_plate", EventFilter.INSTANCE)
            .setPriority(-1000) //todo need to be careful when this is registered, dont want it to trigger before the non-edit mode handlers
            .addListener(EventListener.builder(PlayerBlockPlaceEvent.class)
                    .filter(event -> isFinishPlate(event.getPlayer().getItemInHand(event.getHand())))
                    .ignoreCancelled(true)
                    .handler(FinishPlateBlock::handlePlacement)
                    .build());

    @Override
    public void hook(@NotNull ServerProcess server) {
        ItemManager.register(ID, ITEM);
        server.block().registerHandler(Handler.INSTANCE.getNamespaceId(), () -> Handler.INSTANCE);
        server.eventHandler().addListener(MapWorldRegisterEvent.class, event -> {
            event.getInstance().eventNode().addChild(node);
        });
    }

    private static void handlePlacement(@NotNull PlayerBlockPlaceEvent event) {
        event.setBlock(event.getBlock().withHandler(Handler.INSTANCE));
    }

    private static boolean isFinishPlate(@NotNull ItemStack itemStack) {
        return itemStack.meta().getCustomModelData() == NamedItems.FINISH_PLATE;
    }

    public static class Handler implements BlockHandler {
        private static final BoundingBox BOUNDING_BOX = new BoundingBox(14.0 / 16.0, 1.0 / 16.0, 14.0 / 16.0);

        public static final Handler INSTANCE = new Handler();

        @Override
        public @NotNull NamespaceID getNamespaceId() {
            return NamespaceID.from("mapmaker:finish_plate");
        }

        @Override
        public boolean isTickable() {
            return true;
        }

        @Override
        public void tick(@NotNull Tick tick) {
            var instance = tick.getInstance();
            var pos = tick.getBlockPosition();
            var centerPos = new Vec(pos.blockX() + 0.5, pos.blockY(), pos.blockZ() + 0.5);

            // Check for collision with all players in instance
            var entities = instance.getNearbyEntities(pos, 2);
            for (var entity : entities) {
                if (!(entity instanceof Player player) || !MapHooks.isPlayerPlaying(player)) continue;
                if (!player.getBoundingBox().intersectBox(centerPos.sub(player.getPosition()), BOUNDING_BOX)) continue;

                // Player has stepped on the finish plate, trigger a map completion event
                EventDispatcher.call(new MapWorldCompleteEvent(MapWorld.fromInstance(instance), player));
            }
        }
    }
}
