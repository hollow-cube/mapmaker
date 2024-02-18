package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class CoordinateEditorView extends View {
    private @Outlet("x") Text xText;
    private @Outlet("y") Text yText;
    private @Outlet("z") Text zText;
    private @Outlet("yaw") Text yawText;
    private @Outlet("pitch") Text pitchText;

    private Consumer<Pos> updateFunc;
    private Pos pos;

    public CoordinateEditorView(@NotNull Context context, @NotNull Consumer<Pos> updateFunc, @NotNull Pos initial) {
        super(context);
        this.updateFunc = updateFunc;
        this.pos = initial;

        updateFromPos();
    }

    @Action("x")
    public void handleChangeX() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent(Pos::withX, input), String.valueOf(pos.x())));
    }

    @Action("y")
    public void handleChangeY() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent(Pos::withY, input), String.valueOf(pos.y())));
    }

    @Action("z")
    public void handleChangeZ() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent(Pos::withZ, input), String.valueOf(pos.z())));
    }

    @Action("yaw")
    public void handleChangeYaw() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent((p, yaw) -> p.withYaw(yaw.floatValue()), input), String.valueOf(pos.yaw())));
    }

    @Action("pitch")
    public void handleChangePitch() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent((p, pitch) -> p.withPitch(pitch.floatValue()), input), String.valueOf(pos.pitch())));
    }

    private void safeUpdateComponent(@NotNull BiFunction<Pos, Double, Pos> setter, @NotNull String input) {
        if (input.isEmpty()) return;
        try {
            var value = Double.parseDouble(input);
            pos = setter.apply(pos, value);
            updateFunc.accept(pos);
            updateFromPos();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFromPos() {
        updateSingle(xText, pos.x());
        updateSingle(yText, pos.y());
        updateSingle(zText, pos.z());
        updateSingle(yawText, pos.yaw());
        updateSingle(pitchText, pos.pitch());
    }

    private void updateSingle(@NotNull Text text, double value) {
        text.setText(String.format("%.2f", value));
        text.setArgs(Component.text(value));
    }

}
