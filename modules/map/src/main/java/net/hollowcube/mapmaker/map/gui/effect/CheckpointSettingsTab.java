package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.NotNull;

public class CheckpointSettingsTab extends AbstractEffectSettingsTab<CheckpointEffectData> {

    private @Outlet("checkpoint_lives_inactive") Label checkpointLivesInactiveLabel;
    private @Outlet("checkpoint_lives_active") Label checkpointLivesActiveLabel;
    private @Outlet("checkpoint_lives_switch") Switch checkpointLivesSwitch;

    public CheckpointSettingsTab(@NotNull Context context) {
        super(context);
    }

    @Action("checkpoint_lives_inactive")
    public void handleCheckpointLivesInactive() {
        openCheckpointLivesAnvil();
    }

    @Action("checkpoint_lives_active")
    public void handleCheckpointLivesActive(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK) {
            openCheckpointLivesAnvil();
        } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            data.setLives(CheckpointEffectData.NO_LIVES);
            updateFromData();
        }
    }

    private void openCheckpointLivesAnvil() {
        pushView(context -> new CheckpointLivesAnvil(context, data.lives() ==
                CheckpointEffectData.NO_LIVES ? "" : String.valueOf(data.lives())));
    }

    @Signal(CheckpointLivesAnvil.SIG_UPDATE_NAME)
    public void handleUpdateLives(@NotNull String index) {
        if (index.isEmpty() || "0".equals(index) || "none".equals(index)) {
            data.setLives(CheckpointEffectData.NO_LIVES);
            updateFromData();
            return;
        }

        try {
            var newIndex = Integer.parseInt(index);
            newIndex = MathUtils.clamp(newIndex, 1, 20);
            data.setLives(newIndex);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void updateFromData() {
        super.updateFromData();

        if (data.lives() == CheckpointEffectData.NO_LIVES) {
            checkpointLivesInactiveLabel.setArgs(Component.translatable("gui.checkpoint.lives.none"));
            checkpointLivesSwitch.setOption(0);
        } else {
            checkpointLivesActiveLabel.setArgs(Component.text(data.lives(), TextColor.color(0x30FBFF)));
            checkpointLivesSwitch.setOption(1);
        }
    }
}
