package net.hollowcube.mapmaker.panels.buttons;

import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.MenuBuilder;
import net.hollowcube.mapmaker.panels.Sprite;
import org.jetbrains.annotations.Nullable;

// TODO: should be button overlay
public class LockedButton extends Button {
    public LockedButton(@Nullable String translationKey, int width, int height) {
        super(translationKey, width, height);
    }

    @Override
    public void build(MenuBuilder builder) {
        super.build(builder);

        var lockedSprite = new Sprite("icon2/1_1/lock_overlay", 1, 1);
        builder.draw(lockedSprite.offsetX(), lockedSprite.offsetY(), lockedSprite.sprite());
    }
}
