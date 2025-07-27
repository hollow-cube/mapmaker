package net.hollowcube.mapmaker.editor.gui.biome;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class HexInputAnvil extends View {
    private @Outlet("input") Label inputField;
    private @Outlet("output") Label outputField;

    private final String signal;
    private String input = "";

    public HexInputAnvil(Context context, String signal, @Nullable String startingInput) {
        super(context);
        this.signal = signal;

        if (startingInput == null) {
            startingInput = "";
        }

        input = startingInput;
        inputField.setArgs(Component.text(startingInput));
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(String input) {
        this.input = FontUtil.stripInvalidChars(input);

        // If the name has changed, update the input field to reflect the trimmed string
        if (!this.input.equals(input)) {
            inputField.setArgs(Component.text(this.input));
        }
    }

    @Action("input")
    public void handleBackButton() {
        popView();
    }

    @Action("output")
    public void handleAccept() {
        String trimmedInput = input.trim();

        if (!trimmedInput.isEmpty()) {
            popView(this.signal, trimmedInput);
        } else {
            popView(this.signal, "");
        }
    }

}
