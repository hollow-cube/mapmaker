package net.hollowcube.map.gui.effect.potion;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.entity.potion.PotionEffectList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PotionEffectEditorView extends View {

    private @Outlet("header") Label headerLabel;

    private final PotionEffectList.Entry effect;

    public PotionEffectEditorView(@NotNull Context context, @NotNull PotionEffectList.Entry effect) {
        super(context);
        this.effect = effect;

        //noinspection deprecation
        headerLabel.setItemDirect(effect.type().icon());
    }


    @Action("entries")
    public void handleBuildEntries(@NotNull Pagination.PageRequest<PotionEffectLevelEntry> request) {
        var result = new ArrayList<PotionEffectLevelEntry>();

        if (effect.type().maxLevel() > 1 && effect.type().maxLevel() <= 5) {
            for (int i = 0; i < 5; i++) {
                result.add(new PotionEffectLevelEntry(request.context(), effect, i));
            }
        } else if (effect.type().maxLevel() > 4) {
            for (int i = 0; i < 4; i++) {
                result.add(new PotionEffectLevelEntry(request.context(), effect, i));
            }
            result.add(new PotionEffectLevelEntry(request.context(), effect, -1));
        } else {
            // cant be edited, probably should have some empty switch for this
        }

        request.respond(result, false);
    }

}
