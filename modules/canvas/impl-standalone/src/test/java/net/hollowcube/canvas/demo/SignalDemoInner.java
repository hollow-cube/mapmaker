package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SignalDemoInner extends View {

    public SignalDemoInner(@NotNull Context context) {
        super(context);
    }

    @Action("btn")
    private void handleButton(@NotNull Player player) {
        performSignal("mySignal", "Hello from SignalDemoInner!");
    }

}
