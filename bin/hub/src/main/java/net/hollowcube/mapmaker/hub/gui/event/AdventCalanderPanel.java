package net.hollowcube.mapmaker.hub.gui.event;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.hub.feature.event.EventData;
import net.hollowcube.mapmaker.hub.feature.event.christmas.PresentConstants;
import net.hollowcube.mapmaker.hub.util.HubTime;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.info;

public class AdventCalanderPanel extends Panel {

    private final EventData data;

    public AdventCalanderPanel(Player player) {
        this(EventData.fromPlayer(player));
    }

    public AdventCalanderPanel(EventData data) {
        super(9, 6);

        this.data = data;

        background("event/advent", -15, -34);

        add(0, 0, backOrClose());
        add(1, 0, info("advent"));

        int x = 2;
        int y = 2;

        for (int day = 1; day <= 25; day++) {
            add(x, y, create(day));
            x++;
            if (x > 7) {
                x = 1;
                y++;
            }
        }
    }

    private Button create(int day) {
        var now = HubTime.now();
        var cosmetic = PresentConstants.getRewardForDay(day);
        var isAvailable = now.getMonthValue() == 12 && now.getDayOfMonth() >= day;
        var claimed = this.data.hasPresent(day);

        var button = new Button(null, 1, 1);
        if (cosmetic != null && isAvailable) {
            button.sprite(claimed ? "event/advent_claimed" : "event/advent_available");
            button.from(cosmetic.iconLockedItem());
        } else if (cosmetic != null) {
            button.sprite("event/advent_locked");
        } else  if (claimed) {
            button.sprite("event/advent_cross");
        }

        var dayTranslationKey = "gui.advent.day.%d".formatted(day);
        var hintText = LanguageProviderV2.translateMultiMerged(dayTranslationKey + ".hint", List.of());
        var title = LanguageProviderV2.translate(Component.translatable("gui.advent.day.name", Component.text(day)));

        if (LanguageProviderV2.hasTranslationKey(dayTranslationKey + ".lore")) {
            button.text(title, LanguageProviderV2.translateMulti(dayTranslationKey + ".lore", List.of(hintText)));
        } else {
            button.text(title, LanguageProviderV2.translateMulti("gui.advent.day.lore", List.of(hintText)));
        }
        return button;
    }
}
