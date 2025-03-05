package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.util.ClickType;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;

public class ButtonNode extends GroupNode {
    private Runnable onLeftClick = null;

    public ButtonNode() {
        super("button");
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean changed = super.updateFromProps(props);

        //todo on changed
        if (props.hasMember("onLeftClick")) {
            Value onLeftClick = props.getMember("onLeftClick");
            this.onLeftClick = onLeftClick.canExecute() ? onLeftClick::executeVoid : null;
        } else {
            this.onLeftClick = null;
        }

        return changed;
    }

    @Override
    public boolean handleClick(@NotNull ClickType clickType, int x, int y) {
        if (clickType == ClickType.LEFT && this.onLeftClick != null) {
            this.onLeftClick.run();
            return true;
        }

        return false;
    }
}
