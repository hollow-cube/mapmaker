package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.map.*;
import net.hollowcube.mapmaker.map.instance.MapInstance;
import net.hollowcube.mapmaker.map.util.EventUtil;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.runtime.parkour.item.ResetSaveStateItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleGameplayItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleSpectatorModeItem;
import net.hollowcube.mapmaker.runtime.parkour.setting.OnlySprintSetting;
import net.hollowcube.mapmaker.runtime.polar.ReadWorldAccess2;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
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

        itemRegistry().registerSilent(ResetSaveStateItem.INSTANCE);
        itemRegistry().registerSilent(ToggleSpectatorModeItem.INSTANCE_OFF);
        itemRegistry().registerSilent(ToggleSpectatorModeItem.INSTANCE_ON);
        itemRegistry().registerSilent(ToggleGameplayItem.INSTANCE_OFF);
        itemRegistry().registerSilent(ToggleGameplayItem.INSTANCE_ON);

        eventNode(ParkourState.Playing.class)
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
        // TODO this must be a safe point action also. So can clone the save state or something maybe.

        if (!(getPlayerState(player) instanceof ParkourState.Playing(var saveState, var _)))
            return; //todo need to get cp from spec save state i guess
        var playState = saveState.state(PlayState.class);

        // If they don't have a checkpoint, do a hard rest (todo or are out of lives)
        if (playState.lastState() == null) {
            hardResetPlayer(player); // todo
            return;
        }

        // "pop" the last state to the current
        playState = playState.lastState();
        // todo decrement lives.
        saveState.setState(playState);
        // Create a copy so that we can reset to the checkpoint again
        playState.setLastState(playState.copy());

        // todo this should just be re-setting your state to playing which will re-trigger the play state setup which should tp. not doing it here
        ParkourState.Playing.resetTeleport(player, Objects.requireNonNull(playState.pos()));

//        player.removeTag(COUNTDOWN_END); // Remove so it is reapplied by updatePlayerFromState
//        // Apply the current state to the player and teleport them
//        updatePlayerFromState(world, player, playState);
//        resetTeleport(player, Objects.requireNonNull(playState.pos())).thenRun(() -> {
//            abstractWorld.addPlayerImmediate(player);
//
//            EventDispatcher.call(new MapPlayerInitEvent(world, player, false, false));
//        });

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

    // endregion

    @Override
    protected void loadWorld() {
        var mapData = server().mapService().getMapWorldAsStream(map().id(), false);
        if (mapData == null) return;

        // TODO: loadingworldaccess to configure biomes correctly.
        instance().loadStream(mapData, new ReadWorldAccess2(this));
    }
}
