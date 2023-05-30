package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class SetMapName extends View {
    public static final String SIG_UPDATE_NAME = "set_map_view_update_name";

    private @Outlet("input") Label inputField;
    private @Outlet("output") Label outputField;

    private String input = "";

    public SetMapName(@NotNull Context context) {
        super(context);
        inputField.setArgs(Component.text(""));
    }

    public void showMap(@NotNull String name) {
        input = name;
        inputField.setArgs(Component.text(name));
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        this.input = input;
//        outputField.setArgs(Component.text(input));
    }

    @Action("output")
    public void handleAccept() {
        popView(SIG_UPDATE_NAME, input);
    }
}
