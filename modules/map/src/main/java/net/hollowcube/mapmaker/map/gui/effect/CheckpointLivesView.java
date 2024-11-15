package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.*;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CheckpointLivesView extends View {

    private @Outlet("title") Text titleText;
    private @Outlet("amount") Text amountText;
    private @Outlet("lives_type_switch") Switch livesTypeSwitch;

    public CheckpointLivesView(@NotNull Context context) {
        super(context);

        titleText.setText("Set Lives");
        amountText.setText(""); //TODO whatever the lives are
    }

    private void openCheckpointLivesAnvil() {
        pushView(context -> new CheckpointLivesAnvil(context, String.valueOf(1))); //TODO add data context for lives
    }

    @Signal(CheckpointLivesAnvil.SIG_UPDATE_NAME)
    public void handleUpdateLives(@NotNull String index) { // TODO
//        if (index.isEmpty() || "0".equals(index) || "none".equals(index)) {
//            data.setLives(CheckpointEffectData.NO_LIVES);
//            updateFromData();
//            return;
//        }
//
//        try {
//            var newIndex = Integer.parseInt(index);
//            newIndex = MathUtils.clamp(newIndex, 1, 20);
//            data.setLives(newIndex);
//            updateFromData();
//        } catch (NumberFormatException ignored) {
//        }
    }

    @Action("lives_switch_teleport")
    public void handleLivesSwitchTeleport() {
        livesTypeSwitch.setOption(livesTypeSwitch.getOption() == 1 ? 0 : 1);
    }

    @Action("lives_switch_reset")
    public void handleLivesSwitchReset() {
        livesTypeSwitch.setOption(livesTypeSwitch.getOption() == 1 ? 0 : 1);
    }

    @Action("set_external")
    public void handleSetExternal(@NotNull Player player) {
        //TODO
    }

    @Action("x")
    public void handleChangeX() {
        //TODO
    }

    @Action("y")
    public void handleChangeY() {
        //TODO
    }

    @Action("z")
    public void handleChangeZ() {
        //TODO
    }

    @Action("yaw")
    public void handleChangeYaw() {
        //TODO
    }

    @Action("pitch")
    public void handleChangePitch() {
        //TODO
    }

}
