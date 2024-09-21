package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class StatusSettingsTab extends AbstractEffectSettingsTab<StatusEffectData> {

    private @Outlet("repeatable_switch") Switch repeatableSwitch;
    private @Outlet("repeatable_off") Label repeatableOffLabel;
    private @Outlet("repeatable_on") Label repeatableOnLabel;

    public StatusSettingsTab(@NotNull Context context) {
        super(context);
    }

    @Action("repeatable_off")
    public void toggleRepeatableA() {
        data.setRepeatable(true);
        updateFromData();
    }

    @Action("repeatable_on")
    public void toggleRepeatableB() {
        data.setRepeatable(false);
        updateFromData();
    }

    @Override
    protected void updateFromData() {
        super.updateFromData();

        if (data.repeatable()) {
            repeatableSwitch.setOption(1);
            repeatableOnLabel.setArgs(Component.translatable("gui.status.repeatable.enabled"));
        } else {
            repeatableSwitch.setOption(0);
            repeatableOffLabel.setArgs(Component.translatable("gui.status.repeatable.disabled"));
        }
    }
}
