package net.hollowcube.map.gui.effect;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.effect.StatusEffectData;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class StatusSettingsTab extends AbstractEffectSettingsTab<StatusEffectData> {

    private @Outlet("repeatable") Label repeatableLabel;

    public StatusSettingsTab(@NotNull Context context) {
        super(context);
    }

    @Action("repeatable")
    public void toggleRepeatable() {
        data.setRepeatable(!data.repeatable());
        updateFromData();
    }

    @Override
    protected void updateFromData() {
        super.updateFromData();

        repeatableLabel.setArgs(Component.translatable("gui.status.repeatable." +
                (data.repeatable() ? "enabled" : "disabled")));
    }
}
