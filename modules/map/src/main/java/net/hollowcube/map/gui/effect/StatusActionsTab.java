package net.hollowcube.map.gui.effect;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.effect.BaseEffectData;
import net.hollowcube.map.feature.play.effect.StatusEffectData;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class StatusActionsTab extends AbstractEffectActionsTab<StatusEffectData> {

    private @Outlet("add_time_switch") Switch addTimeSwitch;
    private @Outlet("add_time_off") Label addTimeOffLabel;
    private @Outlet("add_time_on") Label addTimeOnLabel;

    public StatusActionsTab(@NotNull Context context) {
        super(context);
    }

    @Action("add_time_off")
    public void handleChangeAddTimeA() {
        pushView(context -> new BaseEffectTimeLimitAnvil(context, data.extraTime() > 0 ? String.valueOf(data.extraTime() / 1000.0) : ""));
    }

    @Action("add_time_on")
    public void handleChangeAddTimeB() {
        handleChangeAddTimeA();
    }

    @Signal(BaseEffectTimeLimitAnvil.SIG_UPDATE_NAME)
    public void handleUpdateTimeLimit(@NotNull String index) {
        if (index.isEmpty()) {
            data.setExtraTime(BaseEffectData.NO_TIME_LIMIT);
            updateFromData();
            return;
        }

        try {
            var extraTime = (int) (Double.parseDouble(index) * 1000.0);
            if (extraTime < BaseEffectData.NO_TIME_LIMIT || extraTime >= 86_400_000) return;
            data.setExtraTime(extraTime);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }


    @Override
    protected void updateFromData() {
        super.updateFromData();

        if (data.extraTime() <= 0) {
            addTimeSwitch.setOption(0);
            addTimeOffLabel.setArgs(Component.translatable("gui.status.add_time.none"));
        } else {
            addTimeSwitch.setOption(1);
            addTimeOnLabel.setArgs(Component.text(NumberUtil.formatDuration(data.extraTime())));
        }
    }
}
