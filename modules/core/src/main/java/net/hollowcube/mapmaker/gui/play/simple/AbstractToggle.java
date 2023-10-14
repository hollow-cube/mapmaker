package net.hollowcube.mapmaker.gui.play.simple;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class AbstractToggle extends View {
    private static final int STATE_DEFAULT = 0;
    private static final int STATE_SELECTED = 1;

    private @Outlet("state") Switch state;

    private final String signal;

    public AbstractToggle(@NotNull Context context, @NotNull String signal) {
        super(context);
        this.signal = signal;
    }

    public boolean isSelected() {
        return state.getOption() == STATE_SELECTED;
    }

    public void setSelected(boolean selected) {
        state.setOption(selected ? STATE_SELECTED : STATE_DEFAULT);
    }

    @Action("deselected")
    private void clickDeselected() {
        performSignal(signal, true);
    }

    @Action("selected")
    private void clickSelected() {
        performSignal(signal, false);
    }
}
