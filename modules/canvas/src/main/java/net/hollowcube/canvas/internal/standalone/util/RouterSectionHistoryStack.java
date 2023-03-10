package net.hollowcube.canvas.internal.standalone.util;

import net.hollowcube.canvas.HistoryStack;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.section.RouterSection;
import org.jetbrains.annotations.NotNull;

public class RouterSectionHistoryStack implements HistoryStack {
    private final RouterSection router;

    public RouterSectionHistoryStack(@NotNull RouterSection router) {
        this.router = router;
    }

    @Override
    public void push(@NotNull View view) {
        router.push(view.section());
    }

    @Override
    public void pushTransient(@NotNull View view) {
        router.pushTransient(view.section());
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
