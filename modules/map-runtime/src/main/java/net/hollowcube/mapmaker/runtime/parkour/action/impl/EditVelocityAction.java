package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.math.relative.RelativeField;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.velocity.VelocityEditor;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.velocity.VelocityModifier;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.velocity.VelocityModifiers;
import net.kyori.adventure.key.Key;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;

public record EditVelocityAction(
        VelocityModifier modifier
) implements Action {

    public static final Key KEY = Key.key("mapmaker:velocity");
    public static final StructCodec<EditVelocityAction> CODEC = StructCodec.struct(
        "modifier", VelocityModifier.CODEC.optional(VelocityModifiers.DirectionPower.DEFAULT), EditVelocityAction::modifier,
        EditVelocityAction::new
    );

    private static final Sprite SPRITE = new Sprite("action/icon/velocity", 1, 1);

    public static final Editor<EditVelocityAction> EDITOR = new Editor<>(
        it -> it.action() instanceof EditVelocityAction(VelocityModifier modifier) && modifier instanceof VelocityModifiers.DirectionPower ? new VelocityEditor(it) : null,
        SPRITE,
        VelocityEditor::thumbnail
    );

    public EditVelocityAction withYaw(RelativeField yaw) {
        if (this.modifier instanceof VelocityModifiers.DirectionPower simple) {
            return new EditVelocityAction(simple.withYaw(yaw));
        }
        return this;
    }

    public EditVelocityAction withPitch(RelativeField pitch) {
        if (this.modifier instanceof VelocityModifiers.DirectionPower simple) {
            return new EditVelocityAction(simple.withPitch(pitch));
        }
        return this;
    }

    public EditVelocityAction withPower(double power) {
        if (this.modifier instanceof VelocityModifiers.DirectionPower simple) {
            return new EditVelocityAction(simple.withPower(power));
        }
        return this;
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        this.modifier.apply(player);
    }
}
