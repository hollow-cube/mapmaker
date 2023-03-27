package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SignalDemo extends View {

    private @ContextObject Player player;

    public SignalDemo(@NotNull Context context) {
        super(context);
    }

    @Signal("mySignal")
    private void signalReceived(@NotNull String message) {
        //todo some changes to signals
        // - stricter typing on signals, eg define a signal as `public static final Signal<A, B, C> MY_SIGNAL = Signal.create();`
        //   - notably this does not help with the annotated method, which is a shame
        // - Allow a signal method to receive the sender as a parameter

        player.sendMessage("Signal received, switch demo is " + message);
    }

}
