package net.hollowcube.map.gui.effect.potion;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.feature.play.effect.PotionEffectList;
import org.jetbrains.annotations.NotNull;

public class PotionEffectLevelEntry extends View {

    private @Outlet("root") Switch root;

    private final PotionEffectList.Entry effect;
    private final int level;

    public PotionEffectLevelEntry(@NotNull Context context, @NotNull PotionEffectList.Entry effect, int level) {
        super(context);
        this.effect = effect;
        this.level = level;

        root.setOption(level == -1 ? 1 : 0);
    }
}
