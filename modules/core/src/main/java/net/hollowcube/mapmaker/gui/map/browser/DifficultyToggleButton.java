package net.hollowcube.mapmaker.gui.map.browser;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.hollowcube.mapmaker.panels.Button;
import org.jetbrains.annotations.Nullable;

public class DifficultyToggleButton extends Button {
    private final String translation;
    private final String sprite;
    private final int spriteX, spriteY;

    private @Nullable BooleanConsumer onChange;
    private boolean selected = true; // gets changed to off in constructor

    public DifficultyToggleButton(String translation, String sprite, int spriteX, int spriteY) {
        super(translation + ".off", 1, 1);
        this.translation = translation;
        this.sprite = sprite;
        this.spriteX = spriteX;
        this.spriteY = spriteY;

        onLeftClick(_ -> setSelected(!selected));
        setSelected(false);
    }

    public void setSelected(boolean selected) {
        if (this.selected == selected) return;
        this.selected = selected;
        translationKey(translation + (selected ? ".on" : ".off"));
        sprite(sprite, spriteX, spriteY + (selected ? 2 : 0));
        background("generic2/btn/" + (selected ? "selected" : "default") + "/1_1ex");
        disableHoverSprite = selected;
        if (onChange != null) onChange.accept(selected);
    }

    public DifficultyToggleButton onChange(BooleanConsumer onChange) {
        this.onChange = onChange;
        return this;
    }

}
