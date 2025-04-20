package net.hollowcube.mapmaker.scripting.gui.node;

import net.hollowcube.mapmaker.scripting.gui.util.ClickType;
import net.hollowcube.mapmaker.scripting.util.Proxies;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ButtonNode extends GroupNode {
    private Supplier<Value> onLeftClick = null;

    public ButtonNode() {
        super("button");
    }

    @Override
    public boolean updateFromProps(@NotNull Value props) {
        boolean changed = super.updateFromProps(props);

        //todo on changed
        if (props.hasMember("onLeftClick")) {
            Value onLeftClick = props.getMember("onLeftClick");
            this.onLeftClick = onLeftClick.canExecute() ? onLeftClick::execute : null;
        } else {
            this.onLeftClick = null;
        }

        return changed;
    }

    @Override
    public @Nullable CompletableFuture<Void> handleClick(@NotNull ClickType clickType, int x, int y) {
        if (clickType == ClickType.LEFT && this.onLeftClick != null) {
            return Proxies.wrapPromiseLike(this.onLeftClick.get());
        }

        return null;
    }
}
