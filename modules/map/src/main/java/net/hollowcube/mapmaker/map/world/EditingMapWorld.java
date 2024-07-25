package net.hollowcube.mapmaker.map.world;

import com.google.inject.Inject;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.instance.generation.MapGenerators;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.feature.FeatureList;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.item.ItemTags;
import net.hollowcube.mapmaker.map.item.vanilla.DebugStickItem;
import net.hollowcube.mapmaker.map.polar.ReadWorldAccess;
import net.hollowcube.mapmaker.map.polar.ReadWriteWorldAccess;
import net.hollowcube.mapmaker.map.ram.RamUsageOverlay;
import net.hollowcube.mapmaker.map.util.CustomizableHotbarManager;
import net.hollowcube.mapmaker.map.util.MapWorldHelpers;
import net.hollowcube.mapmaker.map.world.savestate.EditState;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.terraform.Terraform;
import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.event.TerraformAxiomLateEnableEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

@SuppressWarnings("UnstableApiUsage")
public class EditingMapWorld extends AbstractMapMakerMapWorld {
    private static final Logger logger = LoggerFactory.getLogger(EditingMapWorld.class);

    private static final int MAP_AUTOSAVE_INTERVAL_SEC = 60 * 5; // 5 minutes

    private final Terraform terraform;

    private final EventNode<InstanceEvent> readWriteNode = EventNode.event("editing-events-rw", EventFilter.INSTANCE, this::canEventWrite)
            .addListener(InstanceTickEvent.class, ev -> tick())
            .addListener(PlayerUseItemEvent.class, this::handleItemUse)
            .addListener(PlayerBlockBreakEvent.class, this::handleBlockBreak)
            .addListener(TerraformAxiomLateEnableEvent.class, this::handleAxiomLateInit);
    private final EventNode<InstanceEvent> readOnlyNode = EventNode.event("editing-events-ro", EventFilter.INSTANCE, Predicate.not(this::canEventWrite))
            .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
            // If customizable hotbar is active, allow that to handle the pre click event.
            .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(!CustomizableHotbarManager.isActive(event.getPlayer())))
            .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
            .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true));

    private volatile TestingMapWorld testWorld = null;

    private final ReentrantLock saveLock = new ReentrantLock();
    private Task autoSaveTask = null;

    @Inject
    public EditingMapWorld(
            @NotNull MapServer server, @NotNull Terraform terraform,
            @NotNull FeatureList features, @NotNull MapData map
    ) {
        super(server, map, features, new MapInstance(map.createDimensionName('e'), map.getSetting(MapSettings.LIGHTING)));
        this.terraform = terraform;

        instance.setGenerator(MapGenerators.voidWorld());
        instance.eventNode().addChild(readWriteNode); // Needs spectators, so register on instance.
        instance.eventNode().addChild(readOnlyNode); // Needs spectators, so register on instance.

        itemRegistry().register(DebugStickItem.INSTANCE);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean canEdit(@NotNull Player player) {
        return isPlaying(player);
    }

    @Override
    public int playerCount() {
        // Need to support the case where the test world is in a different instance
        if (testWorld != null && instance() != testWorld.instance())
            return super.playerCount() + testWorld.playerCount();
        return super.playerCount();
    }

    @Override
    public @Nullable MapWorld playWorld() {
        return testWorld();
    }

    public int memoryUsage() {
        return map().objectUsage();
    }

    public int maxMemoryUsage() {
        return map().objectLimit();
    }

    public void addRamObject() {

    }

    @Override
    protected @Nullable MapWorld getMapForPlayer(@NotNull Player player) {
        if (isPlaying(player)) return this;
        if (testWorld != null && testWorld.isPlaying(player)) return testWorld;
        return null;
    }

    public @Nullable TestingMapWorld testWorld() {
        return testWorld;
    }

    @NonBlocking
    public void enterTestMode(@NotNull Player player) {
        FutureUtil.submitVirtual(() -> enterTestModeInternal(player));
    }

    private void enterTestModeInternal(@NotNull Player player) {
        if (!isPlaying(player) && !isSpectating(player)) {
            logger.error("Player {} tried to enter test mode for {} without being in the map. Currently in {}",
                    player.getUsername(), this, MapWorld.forPlayerOptional(player));
            return;
        }

        if (testWorld == null) {
            testWorld = createTestingWorld();
            testWorld.load();
        }

        // remove from this map (leaving them in the Minestom instance)
        // then add them to the testing world.
        removePlayer(player);
        testWorld.addPlayer(player);
    }

    @Override
    public void load() {
        var mapData = server().mapService().getMapWorld(map().id(), true);
        if (mapData != null) {
            instance.load(mapData, new ReadWorldAccess(this));
        }

        super.load();

        // Kick off autosave
        if (map().verification() == MapVerification.UNVERIFIED) {
            autoSaveTask = instance.scheduler().buildTask(FutureUtil.wrapVirtual(() -> save(true)))
                    .delay(MAP_AUTOSAVE_INTERVAL_SEC, TimeUnit.SECOND)
                    .repeat(MAP_AUTOSAVE_INTERVAL_SEC, TimeUnit.SECOND)
                    .schedule();
        }
    }

    protected @NotNull TestingMapWorld createTestingWorld() {
        return new TestingMapWorld(this);
    }

    public void closeTestWorld() {
        if (testWorld != null) {
            if (!testWorld.isClosed())
                testWorld.close(Component.text("Test world closed"));
            testWorld = null;
        }
    }

    @Override
    public void close(@Nullable Component reason) {
        if (testWorld != null) {
            testWorld.close(reason);
            testWorld = null;
        }

        super.close(reason);

        if (autoSaveTask != null) autoSaveTask.cancel();
        save(false);

        // Unload the backing world
        instance.unload();
    }

    @Blocking
    private void save(boolean isAutoSave) {
        saveLock.lock();
        try {
            if (isAutoSave) logger.info("Autosaving world {}", map().id());

            // Save the map settings
            map().settings().withUpdateRequest(updates -> {
                try {
                    //todo map worlds should have an "owner" which is the owner if they are present, otherwise
                    // it is the first trusted player to join the world. If someone leaves it should find a new
                    // "owner" of the world, or if it cannot (only invited people left), it should close the world.
                    server().mapService().updateMap(map().owner(), map().id(), updates);
                    return true;
                } catch (Exception e) {
                    logger.error("Failed to save map settings for {}", map().id(), e);
                    MinecraftServer.getExceptionManager().handleException(e);
                    return false;
                }
            });

            // Save the world data (if it is unverified only)
            if (map().verification() != MapVerification.PENDING) {
                biomes().write(this);

                var worldData = instance.save(new ReadWriteWorldAccess(this));
                server().mapService().updateMapWorld(map().id(), worldData);
            }

            // Save the players data
            Set.copyOf(players()).forEach(p -> {
                if (!p.isOnline()) {
                    logger.warn("Player {} is not online, removing from map {}", p.getUsername(), map().id());
                    removePlayer(p); // Sanity
                }
            });
            for (var player : players()) {
                var playerData = PlayerDataV2.fromPlayer(player);

                var saveState = SaveState.fromPlayer(player);
                var update = updateSaveState(player, saveState);
                server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), update);
                //todo handle errors

                // Save terraform state
                terraform.saveLocalSession(player, false);
                terraform.savePlayerSession(player, false);
                //todo handle errors here too
            }

            if (isAutoSave) {
                instance.sendMessage(Component.translatable("build.world.save.success"));
            }
        } catch (Exception e) {
            logger.error("Failed to save world {}", map().id(), e);
            instance.sendMessage(Component.translatable("build.world.save.failure"));
        } finally {
            saveLock.unlock();
        }
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        ActionBar.forPlayer(player).addProvider(RamUsageOverlay.INSTANCE);

        var playerData = PlayerDataV2.fromPlayer(player);
        var saveState = MapWorldHelpers.getOrCreateSaveState(this, playerData.id(), SaveStateType.EDITING, EditState.SERIALIZER);
        player.setTag(SaveState.TAG, saveState);

        super.addPlayer(player);

        // Load Terraform State
        terraform.initPlayerSession(player, playerData.id());
        terraform.initLocalSession(player, playerData.id());
        // We don't actually know if Axiom will become present later, so just send the enable message now
        // and if they do have it installed they will get permission to use it.
        Axiom.enable(player);

        player.setPermissionLevel(4);
        player.setGameMode(GameMode.CREATIVE);
        saveState.setPlayStartTime(System.currentTimeMillis());

        // Read from savestate
        var editState = saveState.state(EditState.class);
        editState.inventory().forEach(player.getInventory()::setItemStack);
        player.setHeldItemSlot((byte) editState.selectedSlot());
        player.setFlying(editState.isFlying());
        var storedPos = editState.pos();
        if (storedPos.isEmpty()) {
            // If there is no position stored then this is a fresh edit state so add the builder menu
            player.getInventory().addItemStack(itemRegistry().getItemStack("mapmaker:builder_menu", null));
        }
        FutureUtil.getUnchecked(player.teleport(editState.pos().orElse(map().settings().getSpawnPoint())));
    }

    @Override
    public void removePlayer(@NotNull Player player) {

        // Removing isn't really necessary because it doesn't activate when not in an editing map world,
        // but better safe than sorry.
        ActionBar.forPlayer(player).removeProvider(RamUsageOverlay.INSTANCE);

        try {
            var saveState = SaveState.optionalFromPlayer(player);
            if (saveState != null) {
                //todo handle these errors better
                var playerData = PlayerDataV2.fromPlayer(player);

                var saveStateUpdate = updateSaveState(player, saveState);
                server().mapService().updateSaveState(map().id(), playerData.id(), saveState.id(), saveStateUpdate);

                // Save terraform state
                if (Axiom.isPresent(player)) Axiom.disable(player);
                logger.error("not saving terraform state, need to resolve this");
//                terraform.saveLocalSession(player, true); //todo the player isnt necessarily in the instance anymore.
//                terraform.savePlayerSession(player, true);

                //todo
//                playerData.setLastEditedMap(map.id());

                logger.info("Updated data for {}", player.getUuid());
            }
        } catch (Exception e) {
            logger.error("Failed to save player state for {}", player.getUuid(), e);
        } finally {
            player.removeTag(SaveState.TAG);
            super.removePlayer(player);
        }
    }

    @Override
    public <T> void setTag(@NotNull Tag<T> tag, @Nullable T value) {
        instance().setTag(tag, value);
    }

    @Override
    public void removeTag(@NotNull Tag<?> tag) {
        instance().removeTag(tag);
    }

    private @NotNull SaveStateUpdateRequest updateSaveState(@NotNull Player player, @NotNull SaveState saveState) {
        saveState.updatePlaytime();
        var buildState = saveState.state(EditState.class);
        buildState.setPos(player.getPosition());
        buildState.setFlying(player.isFlying());
        var inventory = new HashMap<Integer, ItemStack>();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            var itemStack = player.getInventory().getItemStack(i);
            if (!itemStack.isAir()) inventory.put(i, itemStack);
        }
        buildState.setInventory(inventory);
        buildState.setSelectedSlot(player.getHeldSlot());
        return saveState.createUpdateRequest();
    }

    private boolean canEventWrite(@NotNull InstanceEvent event) {
        if (event instanceof PlayerEvent playerEvent)
            return canEdit(playerEvent.getPlayer());
        return true;
    }

    private void tick() {
        var minHeight = instance.getCachedDimensionType().minY() - 20;
        for (var player : players()) {
            if (player.getPosition().y() < minHeight) {
                player.teleport(spawnPoint(player));
            }
        }
    }

    private void handleBlockBreak(@NotNull PlayerBlockBreakEvent event) {
        //todo can this become an interaction rule?

        // Prevent swords from breaking blocks
        ItemStack item = event.getPlayer().getItemInMainHand();
        if (ItemTags.SWORDS.contains(item.material().namespace())) {
            event.setCancelled(true);
        }
    }

    private void handleItemUse(@NotNull PlayerUseItemEvent event) {
        //todo can this become an interaction rule?

        // Prevent player from eating suspicious stew
        ItemStack itemStack = event.getItemStack();
        if (itemStack.material() == Material.SUSPICIOUS_STEW) {
            event.setCancelled(true);
        }
    }

    private void handleAxiomLateInit(@NotNull TerraformAxiomLateEnableEvent event) {
        final Player player = event.getPlayer();
        if (!canEdit(player)) return;

        for (final Entity entity : instance.getEntities()) {
            if (entity instanceof MarkerEntity m)
                m.updateNewViewer(player);
        }
    }

    @Override
    public void appendDebugInfo(TextComponent.@NotNull Builder builder) {
        super.appendDebugInfo(builder);

        builder.appendNewline().append(Component.text("  ʜᴀѕ_ᴛᴇѕᴛ: " + (testWorld != null)));
        if (testWorld != null) {
            testWorld.appendDebugInfo(builder);
        }
    }

    @Override
    protected @Nullable BossBar buildBossBarLine1(@NotNull Player player) {
        var builder = Component.text()
                .append(Component.text(FontUtil.rewrite("bossbar_small_1", "building") + " ", NamedTextColor.WHITE))
                .append(Component.text(FontUtil.rewrite("bossbar_ascii_1", map().name()), TextColor.color(0x30FBFF)));

        final String permissionName = player.getUuid().toString().equals(map().owner()) ? "Owner" : "Builder";
        builder.append(Component.text("  " + FontUtil.rewrite("bossbar_small_1", "permission") + " ", TextColor.color(0xB0B0B0)))
                .append(Component.text(FontUtil.rewrite("bossbar_ascii_1", permissionName), NamedTextColor.WHITE));

        return BossBars.createLine1(builder.build());
    }
}
