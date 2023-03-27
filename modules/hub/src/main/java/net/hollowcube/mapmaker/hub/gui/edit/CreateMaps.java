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

    private @Outlet("slot0") EditMapIcon slot0;
    private @Outlet("slot1") EditMapIcon slot1;
    private @Outlet("slot2") EditMapIcon slot2;
    private @Outlet("slot3") EditMapIcon slot3;
    private @Outlet("slot4") EditMapIcon slot4;

    private final PlayerData playerData;
    private final EditMapIcon[] slots;

    public CreateMaps(@NotNull Context context) {
        super(context);
        playerData = PlayerData.fromPlayer(player);

        slots = new EditMapIcon[]{slot0, slot1, slot2, slot3, slot4};

        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            // If the slot is locked, show the lock icon
            if (i >= playerData.getUnlockedMapSlots()) {
                slot.setState(EditMapIcon.State.LOCKED, i, null);
                continue;
            }

            var mapId = playerData.getMapSlot(i);
            // If there is no map here, show the empty icon
            if (mapId == null) {
                slot.setState(EditMapIcon.State.EMPTY, i, null);
                continue;
            }

            // There is a map, show the full icon
            slot.setState(EditMapIcon.State.FULL, i, mapId);
        }
    }

    @Signal(CreateMap.SIG_MAP_CREATED)
    public void mapCreated(int slot, @NotNull MapData map) {
        slots[slot].setState(EditMapIcon.State.FULL, slot, map.getId());
        editor.showMap(map);
        switcher.setState(0);
    }

    @Signal(EditMapIcon.SIG_CREATE_MAP_IN_SLOT)
    public void createMapInSlot(@NotNull EditMapIcon sender, int slot) {
        creator.setSlot(slot);
        switcher.setState(1);
    }

    @Signal(EditMapIcon.SIG_SELECT_MAP_IN_SLOT)
    public void selectMapInSlot(@NotNull MapData map) {
        editor.showMap(map);
        switcher.setState(0);
    }

}
