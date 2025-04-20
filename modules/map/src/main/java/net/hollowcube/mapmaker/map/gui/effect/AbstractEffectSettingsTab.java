package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.*;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AbstractEffectSettingsTab<EffectData extends BaseEffectData> extends View {

    private @Outlet("name_text") Text nameText;
    private @Outlet("progress_index_text") Text progressIndexText;

    private @Outlet("time_limit_inactive") Label timeLimitInactiveLabel;
    private @Outlet("time_limit_active") Label timeLimitActiveLabel;
    private @Outlet("time_limit_switch") Switch timeLimitSwitch;

    private @Outlet("reset_height_inactive") Label resetHeightInactiveLabel;
    private @Outlet("reset_height_active") Label resetHeightActiveLabel;
    private @Outlet("reset_height_switch") Switch resetHeightSwitch;

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
    public void handleChangeName(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK) {
            pushView(context -> new BaseEffectNameAnvil(context, data.hasName() ? data.displayName() : ""));
        } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            data.setName(null);
            updateFromData();
        }
    }

    @Signal(BaseEffectNameAnvil.SIG_UPDATE_NAME)
    public void handleUpdateName(@NotNull String name) {
        name = name.trim();
        if (name.length() > 16) name = name.substring(0, 16);
        if (name.equals(data.displayName())) return;
        data.setName(name.isEmpty() ? null : name);
        updateFromData();
    }

    @Action("progress_index_text")
    public void handleChangeIndex(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK) {
            pushView(context -> new BaseEffectIndexAnvil(context, data.progressIndex() < 1 ? "" : String.valueOf(data.progressIndex())));
        } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            data.setProgressIndex(-1);
            updateFromData();
        }
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
            if (newIndex > 99) return;
            if (newIndex < 1) newIndex = -1;
            data.setProgressIndex(newIndex);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }

    @Action("time_limit_inactive")
    public void handleChangeTimeLimitInactive() {
        openBaseEffectTimeLimitAnvil();
    }

    @Action("time_limit_active")
    public void handleChangeTimeLimitActive(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK) {
            openBaseEffectTimeLimitAnvil();
        } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            data.setTimeLimit(BaseEffectData.NO_TIME_LIMIT);
            updateFromData();
        }
    }

    private void openBaseEffectTimeLimitAnvil() {
        pushView(context -> new BaseEffectTimeLimitAnvil(context, data.timeLimit() > 0 ? String.valueOf((double) data.timeLimit() / 1000) : ""));
    }

    @Signal(BaseEffectTimeLimitAnvil.SIG_UPDATE_NAME)
    public void handleUpdateTimeLimit(@NotNull String index) {
        if (index.isEmpty() || "none".equals(index)) {
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

    @Action("reset_height_inactive")
    public void handleChangeResetHeightInactive() {
        openBaseEffectResetHeightAnvil();
    }

    @Action("reset_height_active")
    public void handleChangeResetHeightActive(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (clickType == ClickType.LEFT_CLICK) {
            openBaseEffectResetHeightAnvil();
        } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
            data.setResetHeight(BaseEffectData.NO_RESET_HEIGHT);
            updateFromData();
        }
    }

    private void openBaseEffectResetHeightAnvil() {
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
            if (newIndex < -64) {
                player().sendMessage(Component.translatable("create_maps.checkpoint.reset_height.too_low"));
                player().closeInventory();
                return;
            } else if (newIndex > adjustedMaxResetHeight(data, maxResetHeight)) {
                player().sendMessage(Component.translatable("create_maps.checkpoint.reset_height.too_high", Component.text(newIndex), Component.text(adjustedMaxResetHeight(data, maxResetHeight))));
                player().closeInventory();
                return;
            }

            data.setResetHeight(newIndex);
            updateFromData();
        } catch (NumberFormatException ignored) {
        }
    }

    static int adjustedMaxResetHeight(@NotNull BaseEffectData data, int maxResetHeight) {
        final var teleport = data.teleport();
        if (teleport != null) return (int) Math.max(maxResetHeight, teleport.y());
        return maxResetHeight;
    }

    protected void updateFromData() {
        if (data.hasName()) {
            nameText.setArgs(Component.text(data.displayName(), TextColor.color(0x30FBFF)));
            nameText.setText(data.displayName());
        } else {
            nameText.setArgs(Component.translatable("gui.effect.name.none"));
            nameText.setText(data.displayName(), TextColor.color(0xEEEEEE));
        }

        if (data.progressIndex() == -1) {
            progressIndexText.setArgs(Component.translatable("gui.effect.progress_index.none"));
            progressIndexText.setText("PI: None");
        } else {
            progressIndexText.setArgs(Component.text(data.progressIndex(), TextColor.color(0x30FBFF)));
            progressIndexText.setText("PI: " + data.progressIndex());
        }

        if (data.timeLimit() == BaseEffectData.NO_TIME_LIMIT) {
            timeLimitInactiveLabel.setArgs(Component.translatable("gui.effect.time_limit.none"));
            timeLimitSwitch.setOption(0);
        } else {
            timeLimitActiveLabel.setArgs(Component.text(NumberUtil.formatDuration(data.timeLimit()), TextColor.color(0x30FBFF)));
            timeLimitSwitch.setOption(1);
        }

        if (data.resetHeight() == BaseEffectData.NO_RESET_HEIGHT) {
            resetHeightInactiveLabel.setArgs(Component.translatable("gui.effect.reset_height.none"));
            resetHeightSwitch.setOption(0);
        } else {
            resetHeightActiveLabel.setArgs(Component.text(data.resetHeight(), TextColor.color(0x30FBFF)));
            resetHeightSwitch.setOption(1);
        }
    }

}
