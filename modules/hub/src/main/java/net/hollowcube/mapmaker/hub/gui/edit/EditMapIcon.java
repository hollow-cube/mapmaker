package net.hollowcube.mapmaker.hub.gui.edit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;

public class EditMapIcon extends View {
    public static final String SIG_CREATE_MAP_IN_SLOT = "create_map_in_slot";
    public static final String SIG_SELECT_MAP_IN_SLOT = "select_map_in_slot";

    public enum State {
        LOCKED,
        EMPTY,
        FULL,
    }

    private @ContextObject MapStorage mapStorage;

    private @Outlet("state") Switch stateSwitch;
    private @Outlet("locked") Label locked;
    private @Outlet("empty") Label empty;
    private @Outlet("full") Label full;

    private int slot = -1;
    private String mapId = null; // Set if state is FULL
    private Future<MapData> mapDataFuture = null;


    public EditMapIcon(@NotNull Context context) {
        super(context);

        // Immediately start loading, we will wait until the state is set using #setState
        setState(Element.State.LOADING);
    }

    public void setState(@NotNull State state, int slot, @Nullable String mapId) {
        Check.argCondition(state == State.FULL && mapId == null, "mapId cannot be null if state is FULL");
        this.slot = slot;
        this.mapId = mapId;
        stateSwitch.setState(state.ordinal());

        // If the state is full, we need to additionally load the map data, otherwise it is ready now
        if (state == State.FULL) {
            mapDataFuture = VIRTUAL_EXECUTOR.submit(() -> {
                var map = mapStorage.getMapById(mapId);
                full.setArgs(
                        Component.text(slot + 1),
                        Component.text(map.getName())
                );
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
            performSignal(SIG_SELECT_MAP_IN_SLOT, mapDataFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
