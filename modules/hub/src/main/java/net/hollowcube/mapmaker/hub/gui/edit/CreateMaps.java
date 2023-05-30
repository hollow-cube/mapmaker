package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMaps extends View {

    private @ContextObject Player player;

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

        boolean hasMaps = false;
        var playerData = PlayerData.fromPlayer(player);
        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            // If the slot is locked, show the lock icon
            if (i >= playerData.getUnlockedMapSlots()) {
                slot.setState(EditMapIconBase.State.LOCKED, i, null);
                continue;
            }

            var mapId = playerData.getMapSlot(i);
            // If there is no map here, show the empty icon
            if (mapId == null) {
                slot.setState(EditMapIconBase.State.EMPTY, i, null);
                continue;
            }

            // There is a map, show the full icon
            slot.setState(EditMapIconBase.State.FULL, i, mapId);
            hasMaps = true;
        }

        // If there are no maps, set them to create the first one.
        if (!hasMaps) createMapInSlot(0);

        //todo if they have a map, we should select it automatically
    }

    @Signal(CreateMap.SIG_MAP_CREATED)
    public void mapCreated(int slot, @NotNull MapData map) {
        slots[slot].setState(EditMapIconBase.State.SELECTED, slot, map.getId());
        editor.showMap(map);
        switcher.setOption(0);
    }

    @Signal(EditMapIconBase.SIG_CREATE_MAP_IN_SLOT)
    public void createMapInSlot(int slot) {
        creator.setSlot(slot);
        switcher.setOption(1);
    }

    @Signal(EditMapIconBase.SIG_SELECT_MAP_IN_SLOT)
    public void selectMapInSlot(@NotNull MapData map) {
        editor.showMap(map);
        switcher.setOption(0);
    }

}
