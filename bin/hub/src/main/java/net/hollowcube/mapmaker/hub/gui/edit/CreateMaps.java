package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.*;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.hub.feature.contest.MapContest;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class CreateMaps extends View {
    private static final PlayerSetting<Integer> SELECTED_SLOT = PlayerSetting.Int("create_maps.selected_slot", 0);

    public static final String SIG_RESET = "create_maps.reset";

    private @ContextObject MapService mapService;
    private @ContextObject PlayerService playerService;
    private @ContextObject Player player;
    private @ContextObject ServerBridge bridge;

    private @Outlet("switch") Switch switcher;
    private @Outlet("editor") EditMap editor;
    private @Outlet("creator") CreateMap creator;
    private @Outlet("contest_creator") CreateContestMap contestCreator;

    private @OutletGroup("slot\\d") EditMapIconBase[] slots;

    private @Outlet("contest_switch") Switch contestSwitcher;
    private @Outlet("contest_slot_locked") Label contestButtonLocked;
    private @Outlet("contest_slot_create") Label contestButtonCreate;
    private @Outlet("contest_slot_in_progress") Label contestButtonInProgress;
    private @Outlet("contest_slot_submitted") Label contestButtonSubmitted;
    private @Outlet("contest_slot_completed") Label contestButtonCompleted;

    private final PlayerData playerData;
    private MapData contestMap;

    public CreateMaps(@NotNull Context context) {
        super(context);
        playerData = PlayerData.fromPlayer(player);

        reset();
    }

    @Signal(CreateMap.SIG_MAP_CREATED)
    public void mapCreated(int slot, @NotNull MapData map) {
        if (slot == MapContest.MAP_CONTEST_SLOT) {
            // Map contest slot is handled slightly differently...
            playerData.setSetting(SELECTED_SLOT, slot);

            this.contestMap = map;
            performSignal(EditMapIconBase.SIG_SELECT_MAP_IN_SLOT, contestMap, MapContest.MAP_CONTEST_SLOT);
            editor.showMap(contestMap, MapContest.MAP_CONTEST_SLOT);
            switcher.setOption(0);
            playerData.setSetting(SELECTED_SLOT, MapContest.MAP_CONTEST_SLOT);

            var pd = MapPlayerData.fromPlayer(player);
            pd.setContestSlot(map.id());

            return;
        }

        // Need to 'predict' that the map will now be in the slot since we likely haven't received an update from remote.
        var pd = MapPlayerData.fromPlayer(player);
        pd.mapSlots(playerData)[slot] = map.id();

        // Also update GUI to reflect the change
        slots[slot].setToSelected(map);
        editor.showMap(map, slot);
        switcher.setOption(0);
        playerData.setSetting(SELECTED_SLOT, slot);
    }

    @Signal(EditMapIconBase.SIG_CREATE_MAP_IN_SLOT)
    public void createMapInSlot(int slot) {
        creator.setSlot(slot);
        switcher.setOption(1);
    }

    @Signal(EditMapIconBase.SIG_SELECT_MAP_IN_SLOT)
    public void selectMapInSlot(@NotNull MapData map, int slot) {
        editor.showMap(map, slot);
        switcher.setOption(0);
        playerData.setSetting(SELECTED_SLOT, slot);
    }

    @Signal(SIG_RESET)
    public void reset() {
        int firstSlot = -1;
        var playerData = MapPlayerData.fromPlayer(player);
        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            // There is a map, show the full icon even if they dont have it unlocked.
            // It means they used to and have a map in that slot.
            var mapId = playerData.getMapSlot(i);
            if (mapId != null) {
                slot.setState(playerData, EditMapIconBase.State.FULL, i, mapId);
                if (firstSlot == -1) firstSlot = i;
                continue;
            }

            // If the slot is locked, show the lock icon
            if (i >= this.playerData.mapSlots()) {
                slot.setState(playerData, EditMapIconBase.State.LOCKED, i, null);
                continue;
            }

            // If there is no map here, show the empty icon
            slot.setState(playerData, EditMapIconBase.State.EMPTY, i, null);
        }

        // If there are no maps, set them to create the first one.
        if (firstSlot == -1) {
            createMapInSlot(0);
        } else {
            // Otherwise, select the slot we have saved for them, otherwise the first slot with a map
            var savedSlot = this.playerData.getSetting(SELECTED_SLOT);
            if (savedSlot != MapContest.MAP_CONTEST_SLOT) {
                var slot = slots[savedSlot].getSlotState() == EditMapIconBase.State.FULL ? savedSlot : firstSlot;
                async(() -> {
                    var map = slots[slot].mapDataFuture.get();
                    selectMapInSlot(map, slot);
                    slots[slot].setToSelected(map);
                });
            }
            // if you have map contest selected we handle it below.
        }

        // Map contest
        var now = LocalDateTime.now();
        long millisToUnlock = ChronoUnit.MILLIS.between(now, MapContest.BUTTON_UNLOCK_DATE);
        long millisToStart = ChronoUnit.MILLIS.between(now, MapContest.START_DATE);
        long millisToEnd = ChronoUnit.MILLIS.between(now, MapContest.END_DATE);

        // idk if there is a better way to write this its kinda wack :sob:
        int status = 0;
        if (millisToEnd <= 0) {
            status = 5; // Contest over, always this status
        } else if (millisToUnlock <= 0) {
            if (millisToStart <= 0) {
                if (playerData.getContestSlot() != null) {
                    if (contestMap != null && contestMap.isPublished()) {
                        status = 4; // Map submitted
                    } else status = 3; // Map in progress
                } else status = 2; // Contest started, map not created
            } else status = 1; // Contest not yet started
        }
        contestSwitcher.setOption(status);
        if (millisToEnd > 0 && playerData.getContestSlot() != null && contestMap == null) {
            contestSwitcher.setState(State.LOADING);
            async(() -> {
                contestMap = mapService.getMap(playerData.id(), playerData.getContestSlot());
                player.scheduleNextTick(_ -> {
                    contestSwitcher.setState(State.ACTIVE);
                    if (contestMap.isPublished())
                        contestSwitcher.setOption(4); // Map submitted

                    var savedSlot = this.playerData.getSetting(SELECTED_SLOT);
                    if (savedSlot == MapContest.MAP_CONTEST_SLOT) {
                        if (contestMap != null && !contestMap.isPublished())
                            selectContestMap(player);
                        else {
                            this.playerData.resetSetting(SELECTED_SLOT);
                            reset(); // Reselect slot 0
                        }
                    }
                });
            });
        }

        contestButtonLocked.setArgs(Component.text(NumberUtil.formatPlayerPlaytime(millisToStart)));
        var timeToEndComponent = Component.text(NumberUtil.formatPlayerPlaytime(millisToEnd));
        contestButtonCreate.setArgs(timeToEndComponent);
        contestButtonInProgress.setArgs(timeToEndComponent);
        contestButtonSubmitted.setArgs(timeToEndComponent);
    }

    @Blocking
    @Action("personal_world")
    public void enterPersonalWorld(@NotNull Player player) {
        var spawnMapId = MapData.SPAWN_MAP_ID;
        if (spawnMapId == null) return;

        if (!CoreFeatureFlags.SPAWN_MAP_ACCESS.test(player))
            return;

        try {
            bridge.joinMap(player, spawnMapId, ServerBridge.JoinMapState.EDITING, "hardcoded_spawn_world");
        } catch (Exception e) {
            player.sendMessage(Component.translatable("generic.map.edit.fail"));
            ExceptionReporter.reportException(e, player);
        } finally {
            player.closeInventory();
        }
    }

    @Signal(Element.SIG_UNMOUNT)
    private void onUnmount() {
        FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(playerService));
    }

    @Action("contest_slot_create")
    public void createContestMap(@NotNull Player player) {
        long millisToStart = ChronoUnit.MILLIS.between(LocalDateTime.now(), MapContest.START_DATE);
        if (millisToStart > 0) return; // Sanity check

        switcher.setOption(2);
    }

    @Action("contest_slot_in_progress")
    public void selectContestMap(@NotNull Player player) {
        if (contestMap == null) return; // Sanity check

        performSignal(EditMapIconBase.SIG_SELECT_MAP_IN_SLOT, contestMap, MapContest.MAP_CONTEST_SLOT);
        editor.showMap(contestMap, MapContest.MAP_CONTEST_SLOT);
        switcher.setOption(0);
        playerData.setSetting(SELECTED_SLOT, MapContest.MAP_CONTEST_SLOT);
    }

}
