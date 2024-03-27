package net.hollowcube.mapmaker.gui.play.details;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TimesTopTenView extends View {
    private static final int START_OFFSET = 3;

    private @OutletGroup("label_.+") Text[] labels;

    public TimesTopTenView(@NotNull Context context) {
        super(context);
    }

    @Blocking
    public void fillEntries(@NotNull List<LeaderboardData.Entry> entries) {
        for (int i = 0; i < labels.length; i++) {
            int index = i + START_OFFSET;

            var model = i % 2 == 0 ? DetailsTimesTabView.MODEL_8X_OFFSET_1 : DetailsTimesTabView.MODEL_8X_OFFSET_2;
            if (index < entries.size()) {
                var entry = entries.get(index);
                labels[i].setItemSprite(DetailsTimesTabView.getPlayerHead2d(entry.player(), model));
                labels[i].setText(NumberUtil.formatMapPlaytime(entry.score(), true));
                labels[i].setArgs(Component.text(player().getUsername()), Component.text(NumberUtil.formatMapPlaytime(entry.score(), true)));
            } else {
                labels[i].setItemSprite(DetailsTimesTabView.getPlayerHead2d(null, model));
                labels[i].setText(DetailsTimesTabView.MISSING_TIME);
                labels[i].setArgs(Component.text("N/A"), Component.text("--:--:--"));
            }
        }
    }
}
