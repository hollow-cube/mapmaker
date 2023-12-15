package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ConfirmAction extends View {
    private @NotNull Runnable confirmationCallback;

    private @Outlet("yes") Label yesButton;
    private @Outlet("no") Label noButton;

    public ConfirmAction(@NotNull Context context, @NotNull Runnable confirmationCallback) {
        super(context);
        this.confirmationCallback = confirmationCallback;
    }

    @Action("yes")
    private void handleYesClick() {
        confirmationCallback.run();
        System.out.println("skibidi bop bop bop skibidi yes yes yes");
    }

    @Action("no")
    private void handleNoClick() {
        popView();
        System.out.println("no");
    }
}