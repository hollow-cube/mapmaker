package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.*;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMaps extends View {
    private static final PlayerSetting<Integer> SELECTED_SLOT = PlayerSetting.Int("create_maps.selected_slot", 0);

    public static final String SIG_RESET = "create_maps.reset";

    private @ContextObject PlayerService playerService;
    private @ContextObject Player player;
    private @ContextObject ServerBridge bridge;

    private @Outlet("switch") Switch switcher;
    private @Outlet("editor") EditMap editor;
    private @Outlet("creator") CreateMap creator;

    private @OutletGroup("slot\\d") EditMapIconBase[] slots;

    private final PlayerDataV2 playerData;

    public CreateMaps(@NotNull Context context) {
        super(context);
        playerData = PlayerDataV2.fromPlayer(player);

        reset();
    }

    @Signal(CreateMap.SIG_MAP_CREATED)
    public void mapCreated(int slot, @NotNull MapData map) {
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

            // If the slot is locked, show the lock icon
            if (i >= playerData.unlockedSlots()) {
                slot.setState(playerData, EditMapIconBase.State.LOCKED, i, null);
                continue;
            }

            var mapId = playerData.getMapSlot(i);
            // If there is no map here, show the empty icon
            if (mapId == null) {
                slot.setState(playerData, EditMapIconBase.State.EMPTY, i, null);
                continue;
            }

            // There is a map, show the full icon
            slot.setState(playerData, EditMapIconBase.State.FULL, i, mapId);
            if (firstSlot == -1) firstSlot = i;
        }

        // If there are no maps, set them to create the first one.
        if (firstSlot == -1) {
            createMapInSlot(0);
        } else {
            // Otherwise, select the slot we have saved for them, otherwise the first slot with a map
            var savedSlot = this.playerData.getSetting(SELECTED_SLOT);
            var slot = slots[savedSlot].getSlotState() == EditMapIconBase.State.FULL ? savedSlot : firstSlot;
            async(() -> {
                var map = slots[slot].mapDataFuture.get();
                selectMapInSlot(map, slot);
                slots[slot].setToSelected(map);
            });
        }
    }

    @Action("personal_world")
    public void enterPersonalWorld(@NotNull Player player) {
        var spawnMapId = MapData.SPAWN_MAP_ID;
        if (spawnMapId == null) return;

        if (!CoreFeatureFlags.SPAWN_MAP_ACCESS.test(player))
            return;

        try {
            bridge.joinMap(player, spawnMapId, ServerBridge.JoinMapState.EDITING);
        } catch (Exception e) {
            player.sendMessage(Component.translatable("generic.map.edit.fail"));
            MinecraftServer.getExceptionManager().handleException(e);
        } finally {
            player.closeInventory();
        }
    }

    @Signal(Element.SIG_UNMOUNT)
    private void onUnmount() {
        playerData.writeUpdatesUpstream(playerService);
    }

}
