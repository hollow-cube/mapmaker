package net.hollowcube.map.gui.effect.potion;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.entity.potion.PotionEffectList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PotionEffectEditorView extends View {

    private @Outlet("header") Label headerLabel;
    private @Outlet("time_edit") Label timeEditLabel;

    private final PotionEffectList.Entry effect;
    private final Runnable save;

    public PotionEffectEditorView(@NotNull Context context, @NotNull PotionEffectList.Entry effect, @NotNull Runnable save) {
        super(context);
        this.effect = effect;
        this.save = save;

        //noinspection deprecation
        headerLabel.setItemDirect(effect.type().icon());
        updateFromEffect();
    }

    @Action("entries")
    public void handleBuildEntries(@NotNull Pagination.PageRequest<PotionEffectLevelEntry> request) {
        var result = new ArrayList<PotionEffectLevelEntry>();

        if (effect.type().maxLevel() > 1 && effect.type().maxLevel() <= 5) {
            for (int i = 1; i <= 5; i++) {
                result.add(new PotionEffectLevelEntry(request.context(), effect, i, save));
            }
        } else if (effect.type().maxLevel() > 4) {
            for (int i = 1; i <= 4; i++) {
                result.add(new PotionEffectLevelEntry(request.context(), effect, i, save));
            }
            result.add(new PotionEffectLevelEntry(request.context(), effect, -1, save));
        } else {
            // cant be edited, probably should have some empty switch for this
        }

        request.respond(result, false);
    }

    @Action("time_rem_5")
    public void handleRemove5() {
        adjustDuration(-5000);
    }

    @Action("time_rem_1")
    public void handleRemove1() {
        adjustDuration(-1000);
    }

    @Action("time_add_1")
    public void handleAdd1() {
        adjustDuration(1000);
    }

    @Action("time_add_5")
    public void handleAdd5() {
        adjustDuration(5000);
    }

    @Action("time_edit")
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

    private void adjustDuration(int change) {
        effect.setDuration(Math.max(0, effect.duration() + change));
        updateFromEffect();
        save.run();
    }

    private void updateFromEffect() {
        timeEditLabel.setArgs(effect.durationComponent());
    }

    @Signal(Element.SIG_CLOSE)
    public void onClose() {
        save.run();
    }

}
