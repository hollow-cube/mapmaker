package net.hollowcube.mapmaker.map.gui.effect.potion;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.entity.potion.PotionInfo;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PotionEffectTypeEntry extends View {
    private @Outlet("select") Label selectLabel;

    private final PotionEffectList potionList;
    private final PotionInfo type;
    private final Runnable save;

    public PotionEffectTypeEntry(@NotNull Context context, @NotNull PotionEffectList potionList, @NotNull PotionInfo type, @NotNull Runnable save) {
        super(context);
        this.potionList = potionList;
        this.type = type;
        this.save = save;

        //noinspection deprecation
        selectLabel.setItemDirect(type.icon());
    }

    @Action("select")
    public void handleSelectEffect(@NotNull Player player) {
        save.run();
        pushView(c -> new PotionEffectEditorView(c, potionList.getOrCreate(type), save));
    }
}
