package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class EditMapIconBase extends View {
    public static final String SIG_CREATE_MAP_IN_SLOT = "create_map_in_slot";
    public static final String SIG_SELECT_MAP_IN_SLOT = "select_map_in_slot";

    public enum State {
        LOCKED,
        EMPTY,
        FULL,
        SELECTED,
    }

    private @ContextObject MapService mapService;

    private @Outlet("state") Switch stateSwitch;
    private @Outlet("locked") Label locked;
    private @Outlet("empty") Label empty;
    private @Outlet("full") Label full;
    private @Outlet("full_inserted") Label fullInserted;

    private int slot = -1;
    private String mapId = null; // Set if state is FULL
    public Future<MapData> mapDataFuture = null; //todo make me private


    public EditMapIconBase(@NotNull Context context) {
        super(context);

        // Immediately start loading, we will wait until the state is set using #setState
        setState(Element.State.LOADING);
    }

    public void setState(@NotNull MapPlayerData playerData, @NotNull State state, int slot, @Nullable String mapId) {
        Check.argCondition(state == State.FULL && mapId == null, "mapId cannot be null if state is FULL");
        this.slot = slot;
        this.mapId = mapId;
        stateSwitch.setOption(state.ordinal());

        // If the state is full, we need to additionally load the map data, otherwise it is ready now
        if (state == State.FULL || state == State.SELECTED) {
            mapDataFuture = async(() -> {
                var map = mapService.getMap(playerData.id(), Objects.requireNonNull(mapId));
                var mapName = Objects.requireNonNullElse(map.settings().getName(), MapData.DEFAULT_NAME);

                var args = new Component[]{Component.text(slot + 1), Component.text(mapName)};
                full.setArgs(args);
                fullInserted.setArgs(args);
                setState(Element.State.ACTIVE);
                return map;
            });
            // todo super.ephemeralLoad(); // function would load but also handle unmounting gracefully (eg ignore result if unmounted)
        } else {
            var slotArg = Component.text(slot + 1);
            locked.setArgs(slotArg);
            empty.setArgs(slotArg);
            setState(Element.State.ACTIVE);
        }
    }

    public void setToSelected(@NotNull MapData map) {
        mapDataFuture = CompletableFuture.completedFuture(map);
        var args = new Component[]{Component.text(slot + 1),
                Component.text(Objects.requireNonNullElse(map.settings().getName(), MapData.DEFAULT_NAME))};
        full.setArgs(args);
        fullInserted.setArgs(args);
        stateSwitch.setOption(State.SELECTED.ordinal());
    }

    @Action("locked")
    private void handleLockedClick() {
        //todo open purchase menu
        System.out.println("Handle locked click");
    }

    @Action("empty")
    private void handleEmptyClick() {
        performSignal(SIG_CREATE_MAP_IN_SLOT, slot);
    }

    @Action("full")
    private void handleFullClick() {
        try {
            //todo should not select until loaded i guess
            stateSwitch.setOption(State.SELECTED.ordinal());
            performSignal(SIG_SELECT_MAP_IN_SLOT, mapDataFuture.get(), slot);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Signal(SIG_SELECT_MAP_IN_SLOT)
    private void handleSelectMapInSlot(MapData mapData, int slot) {
        if (mapData.id().equals(mapId)) {
            return;
        }

        // Reset to full if selected
        if (stateSwitch.getOption() == State.SELECTED.ordinal()) {
            stateSwitch.setOption(State.FULL.ordinal());
        }
    }

    @Signal(SIG_CREATE_MAP_IN_SLOT)
    private void handleCreateMapInSlot(int slot) {
        if (slot == this.slot) {
            return;
        }

        // Reset to full if selected
        if (stateSwitch.getOption() == State.SELECTED.ordinal()) {
            stateSwitch.setOption(State.FULL.ordinal());
        }
    }

}
