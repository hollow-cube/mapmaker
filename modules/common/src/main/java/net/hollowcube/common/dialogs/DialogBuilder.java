package net.hollowcube.common.dialogs;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAfterAction;
import net.minestom.server.dialog.DialogMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class DialogBuilder {

    private final DialogInputsBuilder inputs = DialogInputsBuilder.create();
    private final DialogBodyBuilder body = DialogBodyBuilder.create();

    private Component title = Component.empty();
    private boolean closeOnEscape = false;

    public static DialogBuilder create() {
        return new DialogBuilder();
    }

    public DialogBuilder title(Component title) {
        this.title = LanguageProviderV2.translate(title);
        return this;
    }

    public DialogBuilder closeOnEscape() {
        this.closeOnEscape = true;
        return this;
    }

    public DialogBuilder inputs(Consumer<DialogInputsBuilder> consumer) {
        consumer.accept(this.inputs);
        return this;
    }

    public DialogBuilder body(Consumer<DialogBodyBuilder> consumer) {
        consumer.accept(this.body);
        return this;
    }

    private DialogMetadata metadata(@NotNull DialogAfterAction action) {
        return new DialogMetadata(
                this.title, this.title,
                this.closeOnEscape, false, action,
                this.body.build(), this.inputs.build()
        );
    }

    public Dialog buildConfirmation(@NotNull Key key, @Nullable CompoundBinaryTag extra) {
        return new Dialog.Confirmation(
                this.metadata(DialogAfterAction.NONE),
                DialogButtons.button(Component.translatable("dialog.generic.save"), 150, key, extra),
                DialogButtons.close(Component.translatable("dialog.generic.close"), 150)
        );
    }

    public Dialog buildNotice(@NotNull Key key, @Nullable CompoundBinaryTag extra) {
        return new Dialog.Notice(
                this.metadata(DialogAfterAction.CLOSE),
                DialogButtons.button(Component.translatable("dialog.generic.close"), 150, key, extra)
        );
    }
}
