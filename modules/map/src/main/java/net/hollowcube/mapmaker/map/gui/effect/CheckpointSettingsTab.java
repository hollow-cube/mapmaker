package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.NotNull;

public class CheckpointSettingsTab extends AbstractEffectSettingsTab<CheckpointEffectData> {

    private @Outlet("checkpoint_lives") Label livesLabel;

    public CheckpointSettingsTab(@NotNull Context context) {
        super(context);
    }

    @Action("checkpoint_lives")
    public void handleChangeLives() {
        pushView(context -> new CheckpointLivesAnvil(context, data.lives() ==
                CheckpointEffectData.NO_LIVES ? "" : String.valueOf(data.lives())));
    }

    @Signal(CheckpointLivesAnvil.SIG_UPDATE_NAME)
    public void handleUpdateLives(@NotNull String index) {
        if (index.isEmpty()) {
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

        livesLabel.setArgs(data.lives() == CheckpointEffectData.NO_LIVES
                ? Component.translatable("gui.checkpoint.lives.none") : Component.text(data.lives(), TextColor.color(0x30FBFF)));
    }
}
