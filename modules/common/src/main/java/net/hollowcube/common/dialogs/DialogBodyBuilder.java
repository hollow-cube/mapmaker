package net.hollowcube.common.dialogs;

import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogBody;
import net.minestom.server.dialog.DialogInput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DialogBodyBuilder {

    private final List<DialogBody> bodies = new ArrayList<>();

    public static DialogBodyBuilder create() {
        return new DialogBodyBuilder();
    }

    public DialogBodyBuilder body(@NotNull DialogBody body) {
        this.bodies.add(body);
        return this;
    }

    public DialogBodyBuilder text(@NotNull Component text) {
        return this.body(new DialogBody.PlainMessage(text, DialogBody.PlainMessage.DEFAULT_WIDTH));
    }

    public DialogBodyBuilder text(@NotNull Component text, int width) {
        return this.body(new DialogBody.PlainMessage(text, width));
    }

    public List<DialogBody> build() {
        return this.bodies;
    }
}
