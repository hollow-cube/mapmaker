package net.hollowcube.mapmaker.gui.play.details;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.hollowcube.mapmaker.util.NumberUtil;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TimesTopThreeView extends View {
    private static final int[] indices = {1, 0, 2}; // Gold in center

    private @OutletGroup("entry_.+") Text[] timeTexts;

    public TimesTopThreeView(@NotNull Context context) {
        super(context);
    }

    @Blocking
    public void fillEntries(@NotNull List<LeaderboardData.Entry> entries) {
        for (int i = 0; i < 3; i++) {
            var index = indices[i];
            var offset = FontUtil.computeOffset(switch (index) {
                case 0 -> 2;
                case 1 -> 2;
                default -> 0;
            });
            if (index < entries.size()) {
                var entry = entries.get(index);
                timeTexts[i].setItemSprite(DetailsTimesTabView.getPlayerHead2d(entry.player(), DetailsTimesTabView.MODEL_8X));
                timeTexts[i].setText(offset + NumberUtil.formatMapPlaytime(entry.score(), true));
            } else {
                timeTexts[i].setItemSprite(DetailsTimesTabView.getPlayerHead2d(null, DetailsTimesTabView.MODEL_8X));
                timeTexts[i].setText(offset + DetailsTimesTabView.MISSING_TIME);
            }
        }
    }
}
