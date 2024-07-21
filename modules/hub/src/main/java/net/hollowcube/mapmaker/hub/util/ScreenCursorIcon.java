package net.hollowcube.mapmaker.hub.util;

import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ScreenCursorIcon implements ActionBar.Provider {
    public static final ScreenCursorIcon INSTANCE = new ScreenCursorIcon();

    private ScreenCursorIcon() {
    }

    @Override
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
        builder.pushColor(TextColor.color(78, 92, 40));
        builder.append(BadSprite.require("icon/new_mouse_left").fontChar() + "");
        builder.popColor();
    }

    @Override
    public int hashCode() {
        return ScreenCursorIcon.class.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass().equals(getClass());
    }

}
