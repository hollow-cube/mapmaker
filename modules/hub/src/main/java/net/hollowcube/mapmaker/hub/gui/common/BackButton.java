package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BackButton extends View {
    public BackButton(@NotNull Context context) {
        super(context);
    }

    @Action("btn")
    public void handleClick(@NotNull Player player) {
        player.closeInventory();
        //todo should pop if present, otherwise close
//        popView();
    }
}
