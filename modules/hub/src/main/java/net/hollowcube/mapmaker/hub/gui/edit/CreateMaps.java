package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.HubHandler;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMaps extends View {
    public static final String SIG_RESET = "create_maps.reset";

    private @ContextObject Player player;
    private @ContextObject("handler") HubHandler mapHandler;

    private @Outlet("switch") Switch switcher;
    private @Outlet("editor") EditMap editor;
    private @Outlet("creator") CreateMap creator;

    private @Outlet("slot0") EditMapIconBase slot0;
    private @Outlet("slot1") EditMapIconBase slot1;
    private @Outlet("slot2") EditMapIconBase slot2;
    private @Outlet("slot3") EditMapIconBase slot3;
    private @Outlet("slot4") EditMapIconBase slot4;

    private final EditMapIconBase[] slots;

    public CreateMaps(@NotNull Context context) {
        super(context);

        slots = new EditMapIconBase[]{slot0, slot1, slot2, slot3, slot4};

        reset();
    }

    @Signal(CreateMap.SIG_MAP_CREATED)
    public void mapCreated(int slot, @NotNull MapData map) {
        slots[slot].setToSelected(map);
        editor.showMap(map, slot);
        switcher.setOption(0);
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
    }

    @Signal(SIG_RESET)
    public void reset() {
        int firstSlot = -1;
        var playerData = PlayerDataV2.fromPlayer(player);
        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            // If the slot is locked, show the lock icon
            if (i >= playerData.getUnlockedMapSlots()) {
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
            var slot = firstSlot;
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

        if (!MapData.SPAWN_MAP_PLAYERS.contains(player.getUuid().toString()))
            return;

        try {
            mapHandler.editMap(player, spawnMapId);
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to edit map")); //todo use translation key
            MinecraftServer.getExceptionManager().handleException(e);
        } finally {
            player.closeInventory();
        }
    }

}
