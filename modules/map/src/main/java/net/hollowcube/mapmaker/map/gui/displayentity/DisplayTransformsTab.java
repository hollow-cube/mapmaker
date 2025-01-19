package net.hollowcube.mapmaker.map.gui.displayentity;

import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.gui.common.anvil.TextInputView;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class DisplayTransformsTab extends View {

    private @ContextObject("display") DisplayEntity display;

    private @Outlet("x") Text xText;
    private @Outlet("y") Text yText;
    private @Outlet("z") Text zText;

    private @Outlet("yaw") Text yawText;
    private @Outlet("pitch") Text pitchText;
    private @Outlet("roll") Text rollText;

    private @Outlet("scaleX") Text scaleXText;
    private @Outlet("scaleY") Text scaleYText;
    private @Outlet("scaleZ") Text scaleZText;

    public DisplayTransformsTab(@NotNull Context context) {
        super(context);

        this.updateText();
    }

    private void updateText() {
        var rotation = getRotation(this.display);
        var scale = getScale(this.display);

        setText(this.xText, this.display.getPosition().x());
        setText(this.yText, this.display.getPosition().y());
        setText(this.zText, this.display.getPosition().z());
        setText(this.yawText, rotation.x());
        setText(this.pitchText, rotation.y());
        setText(this.rollText, rotation.z());
        setText(this.scaleXText, scale.x());
        setText(this.scaleYText, scale.y());
        setText(this.scaleZText, scale.z());
    }

    @Action("x")
    public void handleChangeX() {
        pushNumberInput(this.display.getPosition().x(), (display, value) -> setPos(display, Pos::withX, value));
    }

    @Action("y")
    public void handleChangeY() {
        pushNumberInput(this.display.getPosition().y(), (display, value) -> setPos(display, Pos::withY, value));
    }

    @Action("z")
    public void handleChangeZ() {
        pushNumberInput(this.display.getPosition().z(), (display, value) -> setPos(display, Pos::withZ, value));
    }

    @Action("yaw")
    public void handleChangeYaw() {
        pushNumberInput(getRotation(this.display).x(), (display, value) -> setRot(display, Axis.X, value));
    }

    @Action("pitch")
    public void handleChangePitch() {
        pushNumberInput(getRotation(this.display).y(), (display, value) -> setRot(display, Axis.Y, value));
    }

    @Action("roll")
    public void handleChangeRoll() {
        pushNumberInput(getRotation(this.display).z(), (display, value) -> setRot(display, Axis.Z, value));
    }

    @Action("scaleX")
    public void handleChangeScaleX() {
        pushNumberInput(getScale(this.display).x(), (display, value) -> setScale(display, Axis.X, value));
    }

    @Action("scaleY")
    public void handleChangeScaleY() {
        pushNumberInput(getScale(this.display).y(), (display, value) -> setScale(display, Axis.Y, value));
    }

    @Action("scaleZ")
    public void handleChangeScaleZ() {
        pushNumberInput(getScale(this.display).z(), (display, value) -> setScale(display, Axis.Z, value));
    }

    private static boolean setScale(DisplayEntity entity, Axis type, double value) {
        if (value == 0.0) return false;
        var scale = entity.getEntityMeta().getScale();
        switch (type) {
            case X -> entity.scaleDisplay(new Vec(value, scale.y(), scale.z()));
            case Y -> entity.scaleDisplay(new Vec(scale.x(), value, scale.z()));
            case Z -> entity.scaleDisplay(new Vec(scale.x(), scale.y(), value));
        }
        return true;
    }

    private static boolean setRot(DisplayEntity entity, Axis type, double value) {
        var rot = getRotation(entity);
        switch (type) {
            case X -> entity.rotateDisplay(new Vec(value, rot.y(), rot.z()));
            case Y -> entity.rotateDisplay(new Vec(rot.x(), value, rot.z()));
            case Z -> entity.rotateDisplay(new Vec(rot.x(), rot.y(), value));
        }
        return true;
    }

    private static boolean setPos(DisplayEntity entity, BiFunction<Pos, Double, Pos> updater, double value) {
        var newPos = updater.apply(entity.getPosition(), value);
        if (entity.getInstance().getWorldBorder().inBounds(newPos)) {
            entity.teleport(newPos);
            return true;
        }
        return false;
    }

    private void pushNumberInput(double current, BiPredicate<DisplayEntity, Double> updater) {
        var builder = TextInputView.builder()
                .icon("anvil/ruler")
                .title("Set Value")
                .callback(input -> {
                    try {
                        double value = Double.parseDouble(input);
                        if (updater.test(this.display, value)) {
                            this.updateText();
                            return;
                        }
                    } catch (NumberFormatException ignored) {}
                    this.player().sendMessage(Component.translatable("gui.display_entity.invalid_number"));
                });
        pushView(context -> builder.build(context, String.valueOf(current)));
    }

    private void setText(@NotNull Text text, double value) {
        String raw = String.valueOf(value);
        if (raw.length() > 5) {
            text.setText(raw.substring(0, 5) + "...");
        } else {
            text.setText(raw);
        }
        text.setArgs(Component.text(value));
    }

    private static Vec getScale(DisplayEntity entity) {
        var scale = entity.getEntityMeta().getScale();
        return new Vec(roundTo16(scale.x()), roundTo16(scale.y()), roundTo16(scale.z()));
    }

    private static Vec getRotation(DisplayEntity entity) {
        var meta = entity.getEntityMeta();
        var left = meta.getLeftRotation();
        var right = meta.getRightRotation();
        var angles = new Quaternion(left[0], left[1], left[2], left[3])
                .mulThis(new Quaternion(right[0], right[1], right[2], right[3]))
                .toEulerAngles();
        return new Vec(roundTo16(angles.x()), roundTo16(angles.y()), roundTo16(angles.z()));
    }

    private static double roundTo16(double value) {
        return Math.round(value * 16.0) / 16.0;
    }

    private enum Axis {
        X, Y, Z
    }
}
