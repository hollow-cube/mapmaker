package net.hollowcube.mapmaker.map.gui.effect;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public class CoordinateEditorView extends View {
    private @ContextObject Object updateTarget; // The block position (Point) or entity (Entity) of the checkpoint being edited.

    private @Outlet("title") Text titleText;
    private @Outlet("x") Text xText;
    private @Outlet("y") Text yText;
    private @Outlet("z") Text zText;
    private @Outlet("yaw") Text yawText;
    private @Outlet("pitch") Text pitchText;

    private BaseEffectData data;
    private Consumer<Pos> updateFunc;
    private Pos pos;

    public CoordinateEditorView(@NotNull Context context, @NotNull BaseEffectData data, @NotNull Consumer<Pos> updateFunc, @NotNull Pos initial) {
        super(context);
        this.data = data;
        this.updateFunc = updateFunc;
        this.pos = initial;

        updateFromPos();
        titleText.setText("Set Coordinates");
    }

    @Action("set_external")
    public void handleSetExternal(@NotNull Player player) {
        player.setTag(BaseEffectData.TARGET_PLATE, updateTarget);
        player.sendMessage(MapMessages.COMMAND_SETPRECISECOORDS_BEGIN);
        player.closeInventory();
    }

    @Action("x")
    public void handleChangeX() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent(Pos::withX, input, false), String.valueOf(pos.x())));
    }

    @Action("y")
    public void handleChangeY() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent(Pos::withY, input, true), String.valueOf(pos.y())));
    }

    @Action("z")
    public void handleChangeZ() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent(Pos::withZ, input, false), String.valueOf(pos.z())));
    }

    @Action("yaw")
    public void handleChangeYaw() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent((p, yaw) -> p.withYaw(yaw.floatValue()), input, false), String.valueOf(pos.yaw())));
    }

    @Action("pitch")
    public void handleChangePitch() {
        pushView(context -> new CoordinateInputAnvil(context, input ->
                safeUpdateComponent((p, pitch) -> p.withPitch(pitch.floatValue()), input, false), String.valueOf(pos.pitch())));
    }

    private void safeUpdateComponent(@NotNull BiFunction<Pos, Double, Pos> setter, @NotNull String input, boolean isY) {
        if (input.isEmpty()) return;
        try {
            var value = Double.parseDouble(input);

            if (isY && data.resetHeight() != BaseEffectData.NO_RESET_HEIGHT && value < data.resetHeight()) {
                player().sendMessage(Component.translatable("create_maps.checkpoint.teleport.too_low"));
                return;
            }

            pos = setter.apply(pos, value);
            updateFunc.accept(pos);
            updateFromPos();
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateFromPos() {
        textLengthThreeSlots(xText, pos.x());
        textLengthThreeSlots(yText, pos.y());
        textLengthThreeSlots(zText, pos.z());
        textLengthTwoSlots(yawText, pos.yaw());
        textLengthTwoSlots(pitchText, pos.pitch());
    }

    private void textLengthThreeSlots(@NotNull Text text, double value) {
        StringBuilder displayText = new StringBuilder();
        String raw = String.valueOf(value);

        if (raw.length() > 7) {
            raw = raw.substring(0, 7);
            displayText.append(raw).append("...");
        } else {
            displayText.append(raw);
        }

        text.setText(displayText.toString());
        text.setArgs(Component.text(value));
    }

    private void textLengthTwoSlots(@NotNull Text text, double value) {
        StringBuilder displayText = new StringBuilder();
        String raw = String.valueOf(value);

        if (raw.length() > 4) {
            raw = raw.substring(0, 4);
            displayText.append(raw).append("...");
        } else {
            displayText.append(raw);
        }

        text.setText(displayText.toString());
        text.setArgs(Component.text(value));
    }

}
