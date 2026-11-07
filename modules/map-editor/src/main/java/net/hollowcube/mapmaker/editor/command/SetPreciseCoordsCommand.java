package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.ResetHeightAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.base.CoordinateAction;
import net.hollowcube.mapmaker.editor.parkour.TriggerEditor;
import net.hollowcube.mapmaker.runtime.parkour.block.PressurePlateBlock;
import net.hollowcube.mapmaker.runtime.parkour.marker.TriggerMarkerHandler;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNodeData;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.editor.command.EditorConditions.builderOnly;
import static net.kyori.adventure.text.Component.translatable;

public class SetPreciseCoordsCommand extends CommandDsl {

    private final Argument<Point> coordArgument = Argument.RelativeVec3("position")
            .description("The position to teleport to");
    private final Argument<Float> yawArgument = Argument.Float("yaw")
            .description("The yaw to teleport to");
    private final Argument<Float> pitchArgument = Argument.Float("pitch")
            .description("The pitch to teleport to");

    public SetPreciseCoordsCommand() {
        super("setprecisecoords", "spc");

        setCondition(builderOnly());

        addSyntax(playerOnly(this::updatePreciseCoords));
        addSyntax(playerOnly(this::updatePreciseCoords), coordArgument);
        addSyntax(playerOnly(this::updatePreciseCoords), coordArgument, yawArgument, pitchArgument);
    }

    private void updatePreciseCoords(Player player, CommandContext context) {
        // Ensure they are editing a block
        var updateTarget = player.getTag(CoordinateAction.SPC_EDIT_TAG);
        if (updateTarget == null) {
            player.sendMessage(translatable("command.set_precise_coords.no_target"));
            return;
        }

        // Read the target position from args
        Pos targetPos = player.getPosition();
        if (context.has(coordArgument)) targetPos = targetPos.withCoord(context.get(coordArgument));
        if (context.has(yawArgument)) targetPos = targetPos.withYaw(context.get(yawArgument));
        if (context.has(pitchArgument)) targetPos = targetPos.withPitch(context.get(pitchArgument));

        final Pos pos = targetPos;
        var updated = new AtomicBoolean();
        Consumer<ActionList> updater = data -> {
            // Ensure they dont set the teleport in an invalid position relative to the reset height
            var resetHeight = data.findLast(ResetHeightAction.class);
            if (resetHeight != null && pos.blockY() < resetHeight.value())
                return;

            // We just set all teleport actions to the new position, maybe we should preserve the index
            // but it could change between closing the GUI and running the command so IDK.
            for (int i = 0; i < data.size(); i++) {
                var ref = data.get(i);
                if (ref != null && ref.action() instanceof CoordinateAction<?> action && ref.key().equals(updateTarget.key())) {
                    ref.update(_ -> action.withTarget(RelativePos.abs(pos)));
                }
            }
            updated.set(true);
        };

        if (updateTarget.value() instanceof Point targetBlockPosition) {
            var block = player.getInstance().getBlock(targetBlockPosition);
            if (!(block.handler() instanceof PressurePlateBlock)) return;
            TriggerEditor.editBlock(player.getInstance(), targetBlockPosition, block,
                    data -> updater.accept(data.actions()));
        } else if (updateTarget.value() instanceof MarkerEntity marker) {
            if (!TriggerMarkerHandler.ID.equals(marker.getType())) return;
            var node = marker.getTag(TriggerMarkerHandler.DATA_TAG);
            var data = node.actionData();
            updater.accept(data.actions());
            marker.setData(node.withActionData(data).mergeInto(marker.getData()), player, false);
        } else {
            return;
        }

        if (updated.get()) {
            player.removeTag(CoordinateAction.SPC_EDIT_TAG);
            player.sendMessage(translatable("command.set_precise_coords.success", CoordinateUtil.asTranslationArgs(pos)));
        } else {
            player.sendMessage(translatable("create_maps.checkpoint.teleport.too_low"));
        }

    }

}
