package net.hollowcube.mapmaker.hub.gui.edit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditMapIcon extends View {
    public enum State {
        LOCKED,
        EMPTY,
        FULL,
    }

    private @Outlet("state") Switch stateSwitch;
    private @Outlet("full") Label full;

    private String mapId = null; // Set if state is FULL

    public EditMapIcon() {
        // Immediately start loading, we will wait until the state is set using #setState
        setLoading(true);
    }

    @Contract
    public void setState(@NotNull State state, @Nullable String mapId) {
        Check.argCondition(state == State.FULL && mapId == null, "mapId cannot be null if state is FULL");
        this.mapId = mapId;
        stateSwitch.setState(state.ordinal());

        // If the state is full, we need to additionally load the map data, otherwise it is ready now
        if (state == State.FULL) {
            var mapStorage = HubServer.StaticAbuse.instance.mapStorage();
            Futures.addCallback(mapStorage.getMapById(mapId), new FutureCallback<>() {
                @Override
                public void onSuccess(MapData result) {
                    full.setArgs(Component.text(result.getName()));
                    setLoading(false);
                }

                @Override
                public void onFailure(Throwable t) {
                    throw new RuntimeException(t);
                }
            }, Runnable::run);
            // todo super.ephemeralLoad(); // function would load but also handle unmounting gracefully (eg ignore result if unmounted)
        } else {
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
        //todo open create map menu
        System.out.println("Handle empty click");
    }

    @Action("full")
    private void handleFullClick() {
        //todo open edit map menu
        System.out.println("Handle full click: " + mapId);
    }

}
