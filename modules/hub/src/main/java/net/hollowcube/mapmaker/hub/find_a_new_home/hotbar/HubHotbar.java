package net.hollowcube.mapmaker.hub.find_a_new_home.hotbar;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.hub.gui.edit.CreateMaps;
import net.hollowcube.mapmaker.hub.gui.play.PlayMaps;
import net.hollowcube.mapmaker.hub.gui.play.Query;
import net.hollowcube.mapmaker.hub.world.HubWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class HubHotbar {
    private HubHotbar() {
    }

    private static final EventNode<InstanceEvent> eventNode = EventNode.type("mapmaker:hub/hotbar", EventFilter.INSTANCE)
            .addListener(PlayerUseItemEvent.class, HubHotbar::handleUseItem)
            .addListener(EntityAttackEvent.class, HubHotbar::handleHitPlayer)
            .addListener(ItemDropEvent.class, HubHotbar::handleItemDrop)
            .addListener(InventoryPreClickEvent.class, HubHotbar::handleItemClick);

    private static final int PLAY_ITEM_CMD = BadSprite.SPRITE_MAP.get("tablet").cmd();
    private static final int CREATE_ITEM_CMD = BadSprite.SPRITE_MAP.get("hammer").cmd();

    private static final ItemStack PLAY_MAPS_ITEM = ItemStack.builder(Material.DIAMOND)
            .displayName(Component.translatable("hotbar.lobby.play_maps.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.lobby.play_maps.lore", List.of()))
            .meta(meta -> meta.customModelData(PLAY_ITEM_CMD))
            .build();

    private static final ItemStack CREATE_MAPS_ITEM = ItemStack.builder(Material.DIAMOND)
            .displayName(Component.translatable("hotbar.lobby.create_maps.name"))
            .lore(LanguageProviderV2.translateMulti("hotbar.lobby.create_maps.lore", List.of()))
            .meta(meta -> meta.customModelData(CREATE_ITEM_CMD))
            .build();

    public static @NotNull EventNode<InstanceEvent> eventNode() {
        return eventNode;
    }

    public static void applyToPlayer(@NotNull Player player) {
        player.getInventory().setItemStack(0, PLAY_MAPS_ITEM);
        player.getInventory().setItemStack(1, CREATE_MAPS_ITEM);
    }

    private static void handleUseItem(@NotNull PlayerUseItemEvent event) {
        if (event.getHand() != Player.Hand.MAIN) return;
        handleItem(event.getPlayer(), event.getItemStack().meta().getCustomModelData());
    }

    private static void handleItem(@NotNull Player player, int customModelData) {
        var server = HubWorld.fromInstance(player.getInstance()).server();
        if (customModelData == PLAY_ITEM_CMD) {
            server.newOpenGUI(player, c -> new PlayMaps(c.with(Map.of("query", new Query()))));
        } else if (customModelData == CREATE_ITEM_CMD) {
            server.newOpenGUI(player, CreateMaps::new);
        }
    }

    private static void handleHitPlayer(@NotNull EntityAttackEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getTarget() instanceof Player)) return;
        if (player.getItemInMainHand().meta().getCustomModelData() != CREATE_ITEM_CMD) return;

        player.playSound(Sound.sound(Key.key("item.toy.squeak"), Sound.Source.MASTER, 1f, ThreadLocalRandom.current().nextFloat(0.9f, 1.1f)), Sound.Emitter.self());

        spawnBonkEntity(event.getInstance(), event.getTarget().getPosition(), player);
    }

    private static void spawnBonkEntity(Instance instance, Point position, Player viewer) {
        var random = ThreadLocalRandom.current();
        var bonkEntity = new Entity(EntityType.TEXT_DISPLAY);
        bonkEntity.setNoGravity(true);
        bonkEntity.setAutoViewable(false);
        bonkEntity.addViewer(viewer);
        var meta = (TextDisplayMeta) bonkEntity.getEntityMeta();
        meta.setNotifyAboutChanges(false);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(true);
        meta.setBackgroundColor(0);
        meta.setScale(new Vec(0.75, 0.75, 1));
        meta.setNotifyAboutChanges(true);
        bonkEntity.setInstance(instance, position.add(random.nextDouble(-1, 1), random.nextDouble(2, 2.5), random.nextDouble(-1, 1)));

        bonkEntity.scheduler().buildTask(() -> doBonkAnimation(meta))
                .delay(TaskSchedule.tick(3))
                .repeat(TaskSchedule.tick(4))
                .schedule();
        bonkEntity.scheduleRemove(10, TimeUnit.SERVER_TICK);
    }

    private static void doBonkAnimation(TextDisplayMeta meta) {
        meta.setNotifyAboutChanges(false);
        meta.setText(Component.text("BONK!", NamedTextColor.RED));
        meta.setInterpolationDuration(3);
        meta.setInterpolationStartDelta(0);
        if (meta.getScale().x() == 0.75) {
            meta.setScale(new Vec(1.5, 1.5, 1));
        } else {
            meta.setScale(new Vec(1.2, 1.2, 1));
        }
        meta.setNotifyAboutChanges(true);
    }

    private static void handleItemDrop(@NotNull ItemDropEvent event) {
        event.setCancelled(true);
    }

    private static void handleItemClick(@NotNull InventoryPreClickEvent event) {
        event.setCancelled(true);
    }

}
