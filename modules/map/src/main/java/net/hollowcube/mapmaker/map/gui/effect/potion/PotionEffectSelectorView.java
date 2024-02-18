package net.hollowcube.mapmaker.map.gui.effect.potion;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.entity.potion.PotionInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PotionEffectSelectorView extends View {
    private final PotionEffectList potionList;
    private final Runnable save;

    public PotionEffectSelectorView(@NotNull Context context, @NotNull PotionEffectList potionList, @NotNull Runnable save) {
        super(context);
        this.potionList = potionList;
        this.save = save;
    }

    @Action("entries")
    public void handleBuildEntries(@NotNull Pagination.PageRequest<PotionEffectTypeEntry> request) {
        var result = new ArrayList<PotionEffectTypeEntry>();
        for (var type : PotionInfo.sortedValues()) {
            if (potionList.has(type)) continue;
            result.add(new PotionEffectTypeEntry(request.context(), potionList, type, save));
        }
        request.respond(result, false);
    }
}
