package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMaps extends View {

    private @Outlet("slot0") EditMapIcon slot0;
    private @Outlet("slot1") EditMapIcon slot1;
    private @Outlet("slot2") EditMapIcon slot2;
    private @Outlet("slot3") EditMapIcon slot3;
    private @Outlet("slot4") EditMapIcon slot4;

    private final PlayerData playerData;
    private final EditMapIcon[] slots;

    public CreateMaps(@NotNull Player player) {
        playerData = PlayerData.fromPlayer(player);
        slots = new EditMapIcon[]{slot0, slot1, slot2, slot3, slot4};
    }

    @Override
    public void mount() {
        super.mount();

        for (int i = 0; i < slots.length; i++) {
            var slot = slots[i];

            // If the slot is locked, show the lock icon
            if (i >= playerData.getUnlockedMapSlots()) {
                slot.setState(EditMapIcon.State.LOCKED, null);
                continue;
            }

            var mapId = playerData.getMapSlot(i);
            // If there is no map here, show the empty icon
            if (mapId == null) {
                slot.setState(EditMapIcon.State.EMPTY, null);
                continue;
            }

            // There is a map, show the full icon
            slot.setState(EditMapIcon.State.FULL, mapId);
        }

    }
}
