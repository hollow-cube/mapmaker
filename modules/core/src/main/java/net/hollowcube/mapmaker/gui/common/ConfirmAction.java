package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.NotNull;

// Replaced by ExtraPanels helpers i think.
@Deprecated // when canvas is turbo dumped
public class ConfirmAction extends View {
    private @NotNull Runnable confirmationCallback;

    private @Outlet("yes") Label yesButton;
    private @Outlet("no") Label noButton;

    public ConfirmAction(@NotNull Context context, @NotNull Runnable confirmationCallback, @NotNull TranslatableComponent confirmationText) {
        super(context);
        this.confirmationCallback = confirmationCallback;
        yesButton.setArgs(confirmationText);
    }

    @Action(value = "yes", async = true)
    private void handleYesClick() {
        confirmationCallback.run();
    }

    @Action("no")
    private void handleNoClick() {
        popView();
    }
}