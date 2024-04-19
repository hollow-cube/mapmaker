package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.FontUIBuilder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Future;

public class SwitchElement extends ContainerElement implements Switch, SpriteHolder {
    private int state = 0;

    public SwitchElement(@NotNull ElementContext context, @Nullable String id, int width, int height) {
        super(context, id, width, height);
    }

    protected SwitchElement(@NotNull ElementContext context, @NotNull SwitchElement other) {
        super(context, other);
    }

    @Override
    public boolean isAnyLoading() {
        return this.getState() == State.LOADING || children().get(state).isAnyLoading();
    }

    @Override
    public void setOption(int option) {
        if (this.state == option) return;

        Check.argCondition(option < 0 || option >= children().size(), "Invalid option: " + option);
        this.state = option;
        context.markDirty();
    }

    @Override
    public int getOption() {
        return state;
    }

    @Override
    public @Nullable ItemStack @NotNull [] getContents() {
        if (shouldDelegateDraw()) return super.getContents();
        return children().get(state).getContents();
    }

    @Override
    public void buildTitle(@NotNull FontUIBuilder sb, int x, int y) {
        children().get(state).buildTitle(sb, x, y);
    }

    @Override
    public @Nullable Future<Void> handleClick(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (shouldIgnoreInput()) return null;

        return children().get(state).handleClick(player, slot, clickType);
    }

    @Override
    public void performSignal(@NotNull String name, @NotNull Object... args) {
        children().get(state).performSignal(name, args);
    }

    @Override
    public @NotNull SwitchElement clone(@NotNull ElementContext context) {
        return new SwitchElement(context, this);
    }
}
