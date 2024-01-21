package net.hollowcube.map.gui.effect.potion;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.entity.potion.PotionInfo;
import net.hollowcube.map.feature.play.effect.PotionEffectList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PotionEffectListView extends View {

    private @Outlet("entries") Pagination pagination;

    private final PotionEffectList effectList;

    public PotionEffectListView(@NotNull Context context, @NotNull PotionEffectList effectList) {
        super(context);
        this.effectList = effectList;
    }

    @Action("entries")
    public void handleBuildEntries(@NotNull Pagination.PageRequest<PotionEffectEntry> request) {
        var result = new ArrayList<PotionEffectEntry>();
        for (var type : effectList) {
            result.add(new PotionEffectEntry(request.context(), null, type));
        }
        if (!effectList.isFull()) {
            result.add(new PotionEffectEntry(request.context(), effectList, null));
        }
        request.respond(result, false);
    }

    @Signal(Element.SIG_MOUNT)
    public void handleMount() {
        pagination.reset();
    }

    @Signal(PotionEffectEntry.SIG_REMOVE)
    public void handleRemoveEffect(@NotNull PotionInfo type) {
        effectList.remove(type);
        pagination.reset();
    }

}
