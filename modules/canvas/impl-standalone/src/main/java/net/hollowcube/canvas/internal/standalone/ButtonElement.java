package net.hollowcube.canvas.internal.standalone;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ButtonElement extends LabelElement {

    public ButtonElement(@Nullable String id, int width, int height, @NotNull String translationKey) {
        super(id, width, height, translationKey);
    }

    protected ButtonElement(@NotNull ButtonElement other) {
        super(other);
    }

    @Override
    public @NotNull LabelElement dup() {
        return new ButtonElement(this);
    }
}
