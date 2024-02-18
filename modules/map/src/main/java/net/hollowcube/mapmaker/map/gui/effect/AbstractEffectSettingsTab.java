package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class AbstractEffectSettingsTab<EffectData extends BaseEffectData> extends View {

    private @Outlet("name_text") Text nameText;
    private @Outlet("progress_index_text") Text progressIndexText;

    private @Outlet("time_limit") Label timeLimitLabel;
    private @Outlet("reset_height") Label resetHeightLabel;

    protected EffectData data;
    private int maxResetHeight;

    protected AbstractEffectSettingsTab(@NotNull Context context) {
        super(context);
    }

    public void setData(@NotNull EffectData data, int maxResetHeight) {
        this.data = data;
        this.maxResetHeight = maxResetHeight;

        updateFromData();
    }

    @Action("name_text")
    public void handleChangeName() {
        pushView(context -> new BaseEffectNameAnvil(context, data.hasName() ? data.displayName() : ""));
    }

    @Signal(BaseEffectNameAnvil.SIG_UPDATE_NAME)
    public void handleUpdateName(@NotNull String name) {
        name = name.toLowerCase(Locale.ROOT);
        if (name.isEmpty() || name.equals(data.displayName())) return;
        if (name.length() > 16) name = name.substring(0, 16);
        data.setName(name);
        updateFromData();
    }

    @Action("progress_index_text")
    public void handleChangeIndex() {
        pushView(context -> new BaseEffectIndexAnvil(context, data.progressIndex() == -1 ? "" : String.valueOf(data.progressIndex())));
    }

    @Signal(BaseEffectIndexAnvil.SIG_UPDATE_NAME)
    public void handleUpdateIndex(@NotNull String index) {
        if (index.isEmpty()) {
            data.setProgressIndex(-1);
            updateFromData();
            return;
        }

        try {
            var newIndex = Integer.parseInt(index);
            if (newIndex < 0 || newIndex > 99) return;
            data.setProgressIndex(newIndex);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }

    @Action("time_limit")
    public void handleChangeTimeLimit() {
        pushView(context -> new BaseEffectTimeLimitAnvil(context, data.timeLimit() > 0 ? String.valueOf((double) data.timeLimit() / 1000) : ""));
    }

    @Signal(BaseEffectTimeLimitAnvil.SIG_UPDATE_NAME)
    public void handleUpdateTimeLimit(@NotNull String index) {
        if (index.isEmpty()) {
            data.setTimeLimit(BaseEffectData.NO_TIME_LIMIT);
            updateFromData();
            return;
        }

        try {
            var newTimeLimit = (int) (Double.parseDouble(index) * 1000.0);
            if (newTimeLimit < 0 || newTimeLimit >= 86_400_000) return;
            data.setTimeLimit(newTimeLimit);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }

    @Action("reset_height")
    public void handleChangeResetHeight() {
        pushView(context -> new BaseEffectResetHeightAnvil(context, data.resetHeight() == BaseEffectData.NO_RESET_HEIGHT ? "" : String.valueOf(data.resetHeight())));
    }

    @Signal(BaseEffectResetHeightAnvil.SIG_UPDATE_NAME)
    public void handleUpdateResetHeight(@NotNull String index) {
        if (index.isEmpty()) {
            data.setResetHeight(BaseEffectData.NO_RESET_HEIGHT);
            updateFromData();
            return;
        }

        try {
            var newIndex = Integer.parseInt(index);
            newIndex = MathUtils.clamp(newIndex, -64, maxResetHeight);
            data.setResetHeight(newIndex);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }

    protected void updateFromData() {
        if (data.hasName()) {
            nameText.setArgs(Component.text(data.displayName()));
            nameText.setText(data.displayName());
        } else {
            nameText.setArgs(Component.translatable("gui.effect.name.none"));
            nameText.setText(data.displayName(), TextColor.color(0xB0B0B0));
        }

        if (data.progressIndex() == -1) {
            progressIndexText.setArgs(Component.translatable("gui.effect.progress_index.none"));
            progressIndexText.setText("None");
        } else {
            progressIndexText.setArgs(Component.text(data.progressIndex()));
            progressIndexText.setText(String.valueOf(data.progressIndex()));
        }

        resetHeightLabel.setArgs(data.resetHeight() == BaseEffectData.NO_RESET_HEIGHT ?
                Component.translatable("gui.effect.reset_height.none") : Component.text(data.resetHeight()));
        timeLimitLabel.setArgs(data.timeLimit() == BaseEffectData.NO_TIME_LIMIT ?
                Component.translatable("gui.effect.time_limit.none") : Component.text(NumberUtil.formatDuration(data.timeLimit())));
    }

}
