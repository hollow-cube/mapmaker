package net.hollowcube.map.gui.effect;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FontUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaseEffectTimeLimitAnvil extends View {
    public static final String SIG_UPDATE_NAME = "base_effect.update_time_limit";

    private @Outlet("input") Label inputField;
    private @Outlet("output") Label outputField;

    private String input = "";

    public BaseEffectTimeLimitAnvil(@NotNull Context context, @Nullable String startingInput) {
        super(context);

        if (startingInput == null) {
            startingInput = "";
        }

        input = startingInput;
        inputField.setArgs(Component.text(startingInput));
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
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
            popView(SIG_UPDATE_NAME, trimmedInput);
        } else {
            popView(SIG_UPDATE_NAME, "");
        }
    }

}
