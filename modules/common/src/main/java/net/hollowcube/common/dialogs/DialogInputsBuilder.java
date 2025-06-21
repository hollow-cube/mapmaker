package net.hollowcube.common.dialogs;

import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.DialogInput;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DialogInputsBuilder {

    private final List<DialogInput> inputs = new ArrayList<>();

    public static DialogInputsBuilder create() {
        return new DialogInputsBuilder();
    }

    public DialogInputsBuilder input(DialogInput input) {
        this.inputs.add(input);
        return this;
    }

    public DialogInputsBuilder checkbox(
            @NotNull String id, @NotNull Component label,
            boolean value
    ) {
        return checkbox(id, label, value, "true", "false");
    }

    public DialogInputsBuilder checkbox(
            @NotNull String id, @NotNull Component label,
            boolean value,
            @NotNull String onTrue, @NotNull String onFalse
    ) {
        return input(new DialogInput.Boolean(id, label, value, onTrue, onFalse));
    }

    public DialogInputsBuilder text(
            @NotNull String id, @NotNull Component label,
            @NotNull String value,
            int maxLength,
            int width
    ) {
        return input(new DialogInput.Text(id, width, label, true, value, maxLength, null));
    }

    public DialogInputsBuilder multiline(
            @NotNull String id, @NotNull Component label,
            @NotNull String value,
            int maxLength, int maxLines,
            int width, int height
    ) {
        return input(new DialogInput.Text(
                id, width, label, true, value, maxLength != -1 ? maxLength : Integer.MAX_VALUE,
                new DialogInput.Text.Multiline(maxLines != -1 ? maxLines : null, height)
        ));
    }

    public List<DialogInput> build() {
        return this.inputs;
    }
}
