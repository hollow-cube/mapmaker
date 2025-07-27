package net.hollowcube.mapmaker.runtime;

import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.item.ResetSaveStateItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleSpectatorModeItem;
import net.hollowcube.mapmaker.runtime.polar.ReadWorldAccess2;
import net.minestom.server.entity.Player;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ParkourMapWorld2 extends AbstractMapWorld2<ParkourState, ParkourMapWorld2> {
    private static final Tag<SaveState> PLAY_STATE_TAG = Tag.Transient("parkour_play_state");

    public static @Nullable ParkourMapWorld2 forPlayer(Player player) {
        return MapWorld2.forPlayer(player) instanceof ParkourMapWorld2 w ? w : null;
    }

    public ParkourMapWorld2(MapServer server, MapData map) {
        this(server, map, makeMapInstance(map, 'p'));
    }

    protected ParkourMapWorld2(MapServer server, MapData map, MapInstance instance) {
        super(server, map, instance, ParkourState.class);

        itemRegistry().registerSilent(ToggleSpectatorModeItem.INSTANCE_OFF);
        itemRegistry().registerSilent(ToggleSpectatorModeItem.INSTANCE_ON);
        itemRegistry().registerSilent(ResetSaveStateItem.INSTANCE);

        // Make the entire world readonly to all players inside it (spec or playing doesn't matter)
        eventNode()
                .addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true))
                .addListener(PlayerBlockPlaceEvent.class, event -> event.setCancelled(true))
                .addListener(ItemDropEvent.class, event -> event.setCancelled(true))
                .addListener(InventoryPreClickEvent.class, event -> event.setCancelled(true))
                .addListener(PlayerSwapItemEvent.class, event -> event.setCancelled(true));
    }

    public void setSaveState(Player player, SaveState saveState) {
        player.setTag(PLAY_STATE_TAG, saveState);
    }

    public @Nullable SaveState getSaveState(Player player) {
        return player.getTag(PLAY_STATE_TAG);
    }

    // region Player Lifecycle

    @Override
    protected ParkourState initialState(Player player) {
        var saveState = Objects.requireNonNull(getSaveState(player),
                "Player " + player.getUsername() + " has no save state in ParkourMapWorld2");
        return new ParkourState.Playing(saveState);
    }

    @Override
    public void configurePlayer(AsyncPlayerConfigurationEvent event) {
        super.configurePlayer(event);

        final var player = event.getPlayer();
        final var playerData = PlayerDataV2.fromPlayer(player);
        SaveState saveState;
        try {
            saveState = server().mapService().getLatestSaveState(map().id(), playerData.id(),
                    SaveStateType.PLAYING, PlayState.SERIALIZER);
        } catch (MapService.NotFoundError ignored) {
            // No save state yet, create one locally.
            // We do an upsert to save, so it will be created in the map service at that point.
            saveState = new SaveState(UUID.randomUUID().toString(),
                    map().id(), playerData.id(), SaveStateType.PLAYING,
                    PlayState.SERIALIZER, new PlayState());
            saveState.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));
        }
        player.setTag(PLAY_STATE_TAG, saveState);

        player.setRespawnPoint(Objects.requireNonNullElseGet(
                saveState.state(PlayState.class).pos(),
                () -> map().settings().getSpawnPoint()
        ));
    }

    @Override
    public void removePlayer(Player player) {
        super.removePlayer(player);

        player.removeTag(PLAY_STATE_TAG);
    }

    // endregion

    @Override
    protected void loadWorld() {
        var mapData = server().mapService().getMapWorldAsStream(map().id(), false);
        if (mapData == null) return;

        // TODO: loadingworldaccess to configure biomes correctly.
        instance().loadStream(mapData, new ReadWorldAccess2(this));
    }
}
