package net.hollowcube.mapmaker.panels;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import org.jetbrains.annotations.Nullable;

public class ToggleButton extends Button {
    private final String translation;
    private final String sprite;
    private final int spriteX, spriteY;

    private @Nullable BooleanConsumer onChange;
    private boolean selected = true; // gets changed to off in constructor

    public ToggleButton(int width, int height, String translation, String sprite, int spriteX, int spriteY) {
        super(translation + ".off", width, height);
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
        sprite(sprite + (selected ? "_on" : "_off"), spriteX, spriteY);
        if (onChange != null) onChange.accept(selected);
    }

    public ToggleButton onChange(BooleanConsumer onChange) {
        this.onChange = onChange;
        return this;
    }

    @Override
    public ToggleButton background(@Nullable String sprite) {
        return background(sprite, 0, 0);
    }

    @Override
    public ToggleButton background(@Nullable String sprite, int x, int y) {
        super.background(sprite, x, y);
        return this;
    }

}
