package net.hollowcube.map.feature.experimental.marker;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.feature.MapFeature;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerUseItemOnBlockEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MarkerFeature implements MapFeature {
    private final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:feature/checkpoint", EventFilter.INSTANCE)
            .addListener(PlayerUseItemOnBlockEvent.class, this::handleRightClickOnBlockWithItem)
            .addListener(PlayerSpawnInInstanceEvent.class, this::handlePlayerSpawn);

    private static final int MARKER_ITEM_TAG = 1564;

    @Override
    public @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();

        player.getInventory().addItemStack(ItemStack.builder(Material.BLAZE_ROD)
                .meta(meta -> meta.customModelData(MARKER_ITEM_TAG))
                .displayName(Component.text("Marker Tool"))
                .build());
    }

    private void handleRightClickOnBlockWithItem(@NotNull PlayerUseItemOnBlockEvent event) {
        var itemStack = event.getItemStack();
        if (itemStack.meta().getCustomModelData() != MARKER_ITEM_TAG) return;


        var markerId = UUID.randomUUID();
        var entity = new MarkerEntity(markerId, event.getPosition().add(0.5, 1, 0.5));
        entity.setInstance(event.getPlayer().getInstance(), entity.origin())
                .exceptionally(FutureUtil::handleException);

        event.getPlayer().sendMessage("Added");
    }
}
