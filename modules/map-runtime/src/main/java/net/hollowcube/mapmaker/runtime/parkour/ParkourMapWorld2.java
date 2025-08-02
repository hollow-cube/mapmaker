package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.events.PlayerMoveVehicleEvent;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.action.impl.EditTimerAction;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.runtime.parkour.item.ResetSaveStateItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleGameplayItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleSpectatorModeItem;
import net.hollowcube.mapmaker.runtime.parkour.setting.OnlySprintSetting;
import net.hollowcube.mapmaker.runtime.polar.ReadWorldAccess2;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerMoveEvent;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

import static net.hollowcube.mapmaker.map.feature.play.BaseParkourMapFeatureProvider.COUNTDOWN_END;

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

        itemRegistry().registerSilent(ResetSaveStateItem.INSTANCE);
        itemRegistry().registerSilent(ToggleSpectatorModeItem.INSTANCE_OFF);
        itemRegistry().registerSilent(ToggleSpectatorModeItem.INSTANCE_ON);
        itemRegistry().registerSilent(ToggleGameplayItem.INSTANCE_OFF);
        itemRegistry().registerSilent(ToggleGameplayItem.INSTANCE_ON);

        eventNode(ParkourState.Playing.class)
                .addListener(PlayerMoveEvent.class, event -> initTimerFromMove(event.getPlayer(), event.getNewPosition()))
                .addListener(PlayerMoveVehicleEvent.class, event -> initTimerFromMove(event.getPlayer(), event.getNewPosition()))
                .addChild(OnlySprintSetting.EVENT_NODE);

        // Make the entire world readonly to all players inside it (spec or playing doesn't matter)
        eventNode().addChild(EventUtil.EVENT_NODE);
    }

    public void setSaveState(Player player, SaveState saveState) {
        player.setTag(PLAY_STATE_TAG, saveState);
    }

    public @Nullable SaveState getSaveState(Player player) {
        return player.getTag(PLAY_STATE_TAG);
    }

    public void hardResetPlayer(Player player) {
        var newSaveState = new SaveState(UUID.randomUUID().toString(),
                map().id(), player.getUuid().toString(), SaveStateType.PLAYING,
                PlayState.SERIALIZER, new PlayState());
        newSaveState.setProtocolVersion(ProtocolVersions.getProtocolVersion(player));
        setSaveState(player, newSaveState);
        changePlayerState(player, new ParkourState.Playing(newSaveState, true));
    }

    public void softResetPlayer(Player player) {
        if (!(getPlayerState(player) instanceof ParkourState.Playing(var saveState, var isScorable)))
            return;

        // "pop" the last state to the current
        var newPlayState = OpUtils.map(saveState.state(PlayState.class).lastState(), PlayState::copy);
        if (newPlayState == null) {
            // If they don't have a checkpoint, do a hard rest (todo or are out of lives)
            hardResetPlayer(player); // todo
            return;
        }

        // todo decrement lives.

        // Create a copy so that we can reset to this checkpoint again
        newPlayState.setLastState(newPlayState.copy());

        // Resume playing from this state as a safe point action
        var newSaveState = saveState.copy(newPlayState);
        changePlayerState(player, new ParkourState.Playing(newSaveState, isScorable));
    }

    // region Player Lifecycle

    @Override
    protected ParkourState initialState(Player player) {
        var saveState = Objects.requireNonNull(getSaveState(player), () ->
                "Player " + player.getUsername() + " has no save state in ParkourMapWorld2");
        return new ParkourState.Playing(saveState, true);
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

    private void initTimerFromMove(Player player, Pos newPos) {
        if (!(getPlayerState(player) instanceof ParkourState.Playing(var saveState, var _)))
            return;
        if (saveState.getPlayStartTime() != 0) return;

        var oldPosition = player.getPosition();
        if (Vec.fromPoint(oldPosition).equals(Vec.fromPoint(newPos)))
            return; // Player did not actually move, just turn their head

        // Start the timer.
        saveState.setPlayStartTime(System.currentTimeMillis());

        var timer = saveState.state(PlayState.class).get(EditTimerAction.SAVE_DATA);
        if (timer != null && timer > 0) {
            player.setTag(COUNTDOWN_END, System.currentTimeMillis() + (timer * 50L));
        }
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
