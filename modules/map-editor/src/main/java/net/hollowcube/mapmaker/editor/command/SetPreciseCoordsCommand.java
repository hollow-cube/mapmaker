package net.hollowcube.mapmaker.editor.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.editor.parkour.CheckpointEditor;
import net.hollowcube.mapmaker.editor.parkour.StatusEditor;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.ResetHeightAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.TeleportAction;
import net.hollowcube.mapmaker.runtime.parkour.block.CheckpointPlateBlock;
import net.hollowcube.mapmaker.runtime.parkour.block.StatusPlateBlock;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;

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
        var updateTarget = player.getTag(TeleportAction.SPC_TAG);
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
                if (ref != null && ref.action() instanceof TeleportAction) {
                    ref.update(_ -> new TeleportAction(RelativePos.abs(pos)));
                }
            }
            updated.set(true);
        };

        if (updateTarget instanceof Point targetBlockPosition) {
            var block = player.getInstance().getBlock(targetBlockPosition);
            if (block.handler() instanceof CheckpointPlateBlock) {
                CheckpointEditor.editBlock(player.getInstance(), targetBlockPosition, block,
                        data -> updater.accept(data.actions()));
            } else if (block.handler() instanceof StatusPlateBlock) {
                StatusEditor.editBlock(player.getInstance(), targetBlockPosition, block,
                        data -> updater.accept(data.actions()));
            } else return;
        } else if (updateTarget instanceof MarkerEntity marker) {
            if ("mapmaker:checkpoint".equals(marker.getType())) {
                var data = marker.getTag(CheckpointPlateBlock.ENTITY_DATA_TAG);
                updater.accept(data.actions());
                marker.setTag(CheckpointPlateBlock.ENTITY_DATA_TAG, data);
            }
        } else return;

        if (updated.get()) {
            player.removeTag(TeleportAction.SPC_TAG);
            player.sendMessage(translatable("command.set_precise_coords.success", CoordinateUtil.asTranslationArgs(pos)));
        } else {
            player.sendMessage(translatable("create_maps.checkpoint.teleport.too_low"));
        }

    }

}
