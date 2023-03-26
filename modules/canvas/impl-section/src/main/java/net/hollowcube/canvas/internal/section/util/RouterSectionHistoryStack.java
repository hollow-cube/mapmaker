package net.hollowcube.canvas.internal.section.util;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.section.BaseElement;
import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.util.HistoryStack;
import org.jetbrains.annotations.NotNull;

public class RouterSectionHistoryStack implements HistoryStack {
    private final RouterSection router;

    public RouterSectionHistoryStack(@NotNull RouterSection router) {
        this.router = router;
    }

    @Override
    public void push(@NotNull View view) {
        router.push(((BaseElement) view.element()).section());
    }

    @Override
    public void pushTransient(@NotNull View view) {
        router.pushTransient(((BaseElement) view.element()).section());
    }

    @Override
    public boolean isEmpty() {
        return !router.hasHistory();
    }

    @Override
    public boolean pop() {
        if (router.hasHistory()) {
            router.pop();
            return true;
        }
        return false;
    }
}
