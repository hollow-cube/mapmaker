package net.hollowcube.mapmaker.hub.feature.event.christmas;

import net.hollowcube.mapmaker.hub.gui.event.AdventCalendarPanel;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AdventCalendarItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/calendar"), "calendar");
    public static final Key ID = Key.key("mapmaker:advent_calendar");

    public AdventCalendarItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        AdventCalendarPanel.open(click.player());
    }

}
