package net.hollowcube.mapmaker.map.gui.effect.potion;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.entity.potion.PotionInfo;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class PotionEffectListView extends View {

    private @Outlet("entries") Pagination pagination;

    private final PotionEffectList effectList;
    private final Runnable save;

    public PotionEffectListView(@NotNull Context context, @NotNull PotionEffectList effectList, @NotNull Runnable save) {
        super(context);
        this.effectList = effectList;
        this.save = save;
    }

    @Action("entries")
    public void handleBuildEntries(@NotNull Pagination.PageRequest<PotionEffectEntry> request) {
        var result = new ArrayList<PotionEffectEntry>();
        for (var type : effectList) {
            result.add(new PotionEffectEntry(request.context(), null, type, save));
        }
        if (!effectList.isFull()) {
            result.add(new PotionEffectEntry(request.context(), effectList, null, save));
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
        save.run();
    }

}
