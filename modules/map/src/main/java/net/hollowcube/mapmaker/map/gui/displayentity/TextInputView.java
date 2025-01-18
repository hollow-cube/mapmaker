package net.hollowcube.mapmaker.map.gui.displayentity;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public class TextInputView extends View {

    private @Outlet("title") Text titleText;
    private @Outlet("input") Label inputField;
    private @Outlet("output") Label outputField;

    private final Consumer<String> callback;
    private String input;

    public TextInputView(@NotNull Context context, @NotNull Consumer<String> callback, @Nullable String input) {
        super(context);
        this.callback = callback;
        this.input = Objects.requireNonNullElse(input, "");

        this.titleText.setText("Set Value");
        this.inputField.setArgs(Component.text(this.input));
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        this.input = FontUtil.stripInvalidChars(input);

        // If the name has changed, update the input field to reflect the trimmed string
        if (!this.input.equals(input)) {
            this.inputField.setArgs(Component.text(this.input));
        }
    }

    @Action("input")
    public void handleBackButton() {
        popView();
    }

    @Action("output")
    public void handleAccept() {
        this.callback.accept(this.input.trim());
        popView();
    }

}
