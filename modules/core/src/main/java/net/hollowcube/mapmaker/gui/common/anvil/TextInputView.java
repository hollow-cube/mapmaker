package net.hollowcube.mapmaker.gui.common.anvil;

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

public final class TextInputView extends View {

    private @Outlet("title") Text title;
    private @Outlet("input") Label input;
    private @Outlet("icon") Label icon;
    private @Outlet("output") Label output;

    private final TextInputBuilder<String, TextInputView> settings;

    private String lastInput;

    private TextInputView(@NotNull Context context, @NotNull TextInputBuilder<String, TextInputView> settings, @Nullable String input) {
        super(context);
        this.settings = settings;
        this.lastInput = Objects.requireNonNullElse(input, "");

        this.input.setItemSprite(this.input.getItemDirect().withoutExtraTooltip());
        this.output.setItemSprite(this.output.getItemDirect().withoutExtraTooltip());

        this.title.setText(this.settings.title);
        this.input.setArgs(Component.text(this.lastInput));
        this.icon.setSprite(
                this.settings.icon.fontChar(),
                "minecraft:apple", // TODO(1.21.4)
//                0,
                this.settings.icon.width(),
                this.settings.icon.offsetX(),
                this.settings.icon.rightOffset()
        );
    }

    public static TextInputBuilder<String, TextInputView> builder() {
        return new TextInputBuilder<>(TextInputView::new);
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        this.lastInput = FontUtil.stripInvalidChars(input);
        if (this.lastInput.equals(input)) return;
        this.input.setArgs(Component.text(this.lastInput));
    }

    @Action("input")
    public void handleBackButton() {
        popView();
    }

    @Action(value = "output", async = true)
    public void handleAccept() {
        if (this.settings.callback != null) {
            this.settings.callback.accept(this, this.lastInput.trim());
        } else {
            popView(this.settings.signal, this.lastInput.trim());
        }
    }

}
