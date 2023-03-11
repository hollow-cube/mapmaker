package net.hollowcube.mapmaker.hub.gui.edit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class EditMapIcon extends View {
    public enum State {
        LOCKED,
        EMPTY,
        FULL,
    }

    private @Outlet("state") Switch stateSwitch;
    private @Outlet("locked") Label locked;
    private @Outlet("empty") Label empty;
    private @Outlet("full") Label full;

    private int slot = -1;
    private String mapId = null; // Set if state is FULL
    private ListenableFuture<MapData> mapDataFuture = null;

    private Consumer<MapData> selectMap;
    private IntConsumer createMap;

    public EditMapIcon() {
        // Immediately start loading, we will wait until the state is set using #setState
        setLoading(true);
    }

    public void setCallbacks(@NotNull Consumer<MapData> onSelect, @NotNull IntConsumer onCreate) {
        this.selectMap = onSelect;
        this.createMap = onCreate;
    }

    public void setState(@NotNull State state, int slot, @Nullable String mapId) {
        Check.argCondition(state == State.FULL && mapId == null, "mapId cannot be null if state is FULL");
        this.slot = slot;
        this.mapId = mapId;
        stateSwitch.setState(state.ordinal());

        // If the state is full, we need to additionally load the map data, otherwise it is ready now
        if (state == State.FULL) {
            var mapStorage = HubServer.StaticAbuse.instance.mapStorage();
            mapDataFuture = mapStorage.getMapById(mapId);
            Futures.addCallback(mapDataFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(MapData result) {
                    full.setArgs(
                            Component.text(slot + 1),
                            Component.text(result.getName())
                    );
                    setLoading(false);
                }

                @Override
                public void onFailure(Throwable t) {
                    throw new RuntimeException(t);
                }
            }, Runnable::run);
            // todo super.ephemeralLoad(); // function would load but also handle unmounting gracefully (eg ignore result if unmounted)
        } else {
            var slotArg = Component.text(slot + 1);
            locked.setArgs(slotArg);
            empty.setArgs(slotArg);
            setLoading(false);
        }
    }

    @Action("locked")
    private void handleLockedClick() {
        //todo open purchase menu
        System.out.println("Handle locked click");
    }

    @Action("empty")
    private void handleEmptyClick() {
        createMap.accept(slot);
    }

    @Action("full")
    private void handleFullClick() {
        try {
            //todo should not select until loaded i guess
            selectMap.accept(mapDataFuture.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

}
