package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class ItemEditorView extends View {

    private @Outlet("slots_used") Text slotsUsedText;

    private @Outlet("elytra_cycle_switch") Switch elytraCycleSwitch;
    private @Outlet("elytra_keep_switch") Switch elytraKeepSwitch;
    private @Outlet("elytra_give_switch") Switch elytraGiveSwitch;
    private @Outlet("elytra_take_switch") Switch elytraTakeSwitch;

    enum ElytraState {
        KEEP, GIVE, TAKE
    }

    private ElytraState elytraState = ElytraState.KEEP;

    public ItemEditorView(@NotNull Context context) {
        super(context);

        updateFromState();
    }

    @Action("reset")
    private void reset() {
        setElytraState(ElytraState.KEEP);
    }

    private void updateFromState() {
        slotsUsedText.setText("0/3 Slots Used");

        elytraCycleSwitch.setOption(elytraState.ordinal());
        elytraKeepSwitch.setOption(elytraState == ElytraState.KEEP);
        elytraGiveSwitch.setOption(elytraState == ElytraState.GIVE);
        elytraTakeSwitch.setOption(elytraState == ElytraState.TAKE);
    }

    @Action("elytra_cycle_keep")
    private void elytraCycleKeep() {
        setElytraState(ElytraState.GIVE);
    }

    @Action("elytra_cycle_give")
    private void elytraCycleGive() {
        setElytraState(ElytraState.TAKE);
    }

    @Action("elytra_cycle_take")
    private void elytraCycleTake() {
        setElytraState(ElytraState.KEEP);
    }

    @Action("elytra_keep_off")
    private void elytraKeepOff() {
        setElytraState(ElytraState.KEEP);
    }

    @Action("elytra_give_off")
    private void elytraGiveOff() {
        setElytraState(ElytraState.GIVE);
    }

    @Action("elytra_take_off")
    private void elytraTakeOff() {
        setElytraState(ElytraState.TAKE);
    }

    private void setElytraState(@NotNull ElytraState newState) {
        this.elytraState = newState;
        updateFromState();
    }
}
