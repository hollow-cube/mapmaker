package net.hollowcube.map.feature.checkpoint.gui;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.hollowcube.map.feature.checkpoint.CheckpointFeatureProvider.MINIMUM_RESET_HEIGHT;

public class ResetHeightSetting extends View {

    private @Outlet("toggle") Switch indicatorToggle;
    private @Outlet("indicator_off") Label indicatorOff;
    private @Outlet("indicator_on") Label indicatorOn;

//    private @ContextObject MapData.POI poi;

    public ResetHeightSetting(@NotNull Context context) {
        super(context);
        updateIndicator();
    }

    @Action("indicator_on")
    public void disableResetHeight() {
//        poi.set("active", false);
        updateIndicator();
    }

    @Action("indicator_off")
    public void enableResetHeight() {
//        poi.set("active", true);
        updateIndicator();
    }

    //todo implement min max reset

    @Action("down")
    public void stepDown(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        int delta = switch (clickType) {
            case LEFT_CLICK -> 1;
            case START_SHIFT_CLICK, SHIFT_CLICK -> 5;
            default -> 0;
        };
        int newHeight = Math.max(getResetHeight() - delta, MINIMUM_RESET_HEIGHT);
        setResetHeight(newHeight);
    }

    @Action("up")
    public void stepUp(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        int delta = switch (clickType) {
            case LEFT_CLICK -> 1;
            case START_SHIFT_CLICK, SHIFT_CLICK -> 5;
            default -> 0;
        };
//        int newHeight = Math.min(getResetHeight() + delta, poi.getPos().blockY());
//        setResetHeight(newHeight);
    }

    private int getResetHeight() {
//        return poi.getOrDefault("resetHeight", poi.getPos().blockY() - 5);
        return 0;
    }

    private void setResetHeight(int value) {
//        poi.set("resetHeight", value);
        updateIndicator();
    }

    private void updateIndicator() {
//        var state = poi.getOrDefault("active", false) ? 1 : 0;
//        indicatorToggle.setOption(state);

        var args = List.<Component>of(
                Component.text(getResetHeight())
        );
        indicatorOff.setArgs(args);
        indicatorOn.setArgs(args);
    }


}
