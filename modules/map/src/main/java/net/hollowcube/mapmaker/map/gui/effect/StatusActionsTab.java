package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatusActionsTab extends AbstractEffectActionsTab<StatusEffectData> {

    private @Outlet("add_time_switch") Switch addTimeSwitch;
    private @Outlet("add_time_inactive") Label addTimeInactiveLabel;
    private @Outlet("add_time_active") Label addTimeActiveLabel;

    public StatusActionsTab(@NotNull Context context) {
        super(context);
    }

    @Action("add_time_inactive")
    public void handleChangeAddTimeA() {
        pushView(context -> new BaseEffectTimeLimitAnvil(context, data.extraTime() > 0 ? String.valueOf(data.extraTime() / 1000.0) : ""));
    }

    @Action("add_time_active")
    public void handleChangeAddTimeB(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK || clickType == ClickType.RIGHT_CLICK) {
            handleChangeAddTimeA();
        } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            data.setExtraTime(BaseEffectData.NO_TIME_LIMIT);
            updateFromData();
        }
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
            addTimeInactiveLabel.setArgs(Component.translatable("gui.effects.actions.add_time.none"));
        } else {
            addTimeSwitch.setOption(1);
            addTimeActiveLabel.setArgs(Component.text(NumberUtil.formatDuration(data.extraTime()), TextColor.color(0x30FBFF)));
        }
    }
}
