package net.hollowcube.mapmaker.map.gui.effect.potion;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.utils.MathUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.jetbrains.annotations.NotNull;

public class PotionEffectEditorView extends View {

    private @Outlet("title") Text titleText;
    private @Outlet("header") Text headerText;

    private @Outlet("level") Text levelText;
    private @Outlet("level_dec_switch") Switch levelDecSwitch;
    private @Outlet("level_inc_switch") Switch levelIncSwitch;

    private @Outlet("time") Text timeText;
    private @Outlet("time_dec5_switch") Switch timeDec5Switch;
    private @Outlet("time_dec1_switch") Switch timeDec1Switch;
    private @Outlet("time_inc5_switch") Switch timeInc5Switch;
    private @Outlet("time_inc1_switch") Switch timeInc1Switch;

    private final PotionEffectList.Entry effect;
    private final Runnable save;

    public PotionEffectEditorView(@NotNull Context context, @NotNull PotionEffectList.Entry effect, @NotNull Runnable save) {
        super(context);

        titleText.setText("Potion Editor");

        this.effect = effect;
        this.save = save;

        var effectName = Component.translatable("gui.effect.potion.type." + effect.type().id() + ".name");
        headerText.setText(PlainTextComponentSerializer.plainText().serialize(LanguageProviderV2.translate(effectName)));
        headerText.setArgs(effectName);

        updateFromEffect();
    }

    @Action("reset")
    public void resetToDefault() {
        effect.setLevel(1);
        effect.setDuration(0);
        updateFromEffect();
    }

    @Action("level_dec_on")
    public void handleDecLevel() {
        effect.setLevel(Math.max(1, effect.level() - 1));
        updateFromEffect();
        save.run();
    }

    @Action("level_inc_on")
    public void handleIncLevel() {
        effect.setLevel(Math.min(effect.type().maxLevel(), effect.level() + 1));
        updateFromEffect();
        save.run();
    }

    @Action("time_dec5_on")
    public void handleTimeDec5() {
        adjustDuration(-5000);
    }

    @Action("time_dec1_on")
    public void handleTimeDec1() {
        adjustDuration(-1000);
    }

    @Action("time_inc1_on")
    public void handleTimeInc1() {
        adjustDuration(1000);
    }

    @Action("time_inc5_on")
    public void handleTimeInc5() {
        adjustDuration(5000);
    }

    private void adjustDuration(int change) {
        effect.setDuration(MathUtils.clamp(effect.duration() + change, PotionEffectList.MIN_DURATION_MS, PotionEffectList.MAX_DURATION_MS));
        updateFromEffect();
        save.run();
    }

    @Action("level")
    public void handleSetCustomLevel() {
        pushView(c -> new PotionEffectCustomLevelAnvil(c, String.valueOf(effect.level())));
    }

    @Signal(PotionEffectCustomLevelAnvil.SIG_UPDATE_NAME)
    public void handleUpdateLevelFromInput(@NotNull String input) {
        if (input.isEmpty()) {
            effect.setLevel(1);
            updateFromEffect();
            return;
        }

        try {
            int newLevel = Integer.parseInt(input);
            if (newLevel >= 128) {
                effect.setLevel(128);
            } else effect.setLevel(Math.max(newLevel, 1));
            updateFromEffect();
            save.run();
        } catch (NumberFormatException ignored) {
        }
    }

    @Action("time")
    public void handleSetCustomDuration() {
        pushView(c -> new PotionEffectCustomDurationAnvil(c, String.valueOf(effect.duration() / 1000.0)));
    }

    @Signal(PotionEffectCustomDurationAnvil.SIG_UPDATE_NAME)
    public void handleUpdateDurationFromInput(@NotNull String input) {
        if (input.isEmpty()) {
            effect.setDuration(0);
            updateFromEffect();
            return;
        }

        try {
            var newDuration = (int) (Double.parseDouble(input) * 1000.0);
            effect.setDuration(Math.max(0, newDuration));
            updateFromEffect();
            save.run();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFromEffect() {
        levelText.setText(String.valueOf(effect.level()));
        levelText.setArgs(Component.text(String.valueOf(effect.level())));
        levelDecSwitch.setOption(effect.level() > 1);
        levelIncSwitch.setOption(effect.level() < effect.type().maxLevel());

        timeText.setArgs(effect.durationComponent());
        timeText.setText(PlainTextComponentSerializer.plainText().serialize(effect.durationComponent()));
        timeDec5Switch.setOption(effect.duration() >= PotionEffectList.MIN_DURATION_MS + 5000);
        timeDec1Switch.setOption(effect.duration() >= PotionEffectList.MIN_DURATION_MS + 1000);
        timeInc5Switch.setOption(effect.duration() < PotionEffectList.MAX_DURATION_MS - 5000);
        timeInc1Switch.setOption(effect.duration() < PotionEffectList.MAX_DURATION_MS - 1000);
    }

    @Signal(Element.SIG_CLOSE)
    public void onClose() {
        save.run();
    }

}
