package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.impl.ResetHeightAction;
import net.hollowcube.mapmaker.map.action.impl.TeleportAction;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.map.util.RelativePos;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class SetPreciseCoordsCommand extends CommandDsl {

    private final Argument<Point> coordArgument = Argument.RelativeVec3("position")
            .description("The position to teleport to");
    private final Argument<Float> yawArgument = Argument.Float("yaw")
            .description("The yaw to teleport to");
    private final Argument<Float> pitchArgument = Argument.Float("pitch")
            .description("The pitch to teleport to");

    public SetPreciseCoordsCommand() {
        super("setprecisecoords", "spc");

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::updatePreciseCoords));
        addSyntax(playerOnly(this::updatePreciseCoords), coordArgument);
        addSyntax(playerOnly(this::updatePreciseCoords), coordArgument, yawArgument, pitchArgument);
    }

    private void updatePreciseCoords(@NotNull Player player, @NotNull CommandContext context) {
        // Ensure they are editing a block
        var updateTarget = player.getTag(TeleportAction.SPC_TAG);
        if (updateTarget == null) {
            player.sendMessage(MapMessages.COMMAND_SETPRECISECOORDS_NO_TARGET);
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
                    ref.update(_ -> new TeleportAction(new RelativePos(pos, RelativeFlags.NONE)));
                }
            }
            updated.set(true);
        };

        if (updateTarget instanceof Point targetBlockPosition) {
            var block = player.getInstance().getBlock(targetBlockPosition);
            if (block.handler() instanceof CheckpointPlateBlock cp) {
                cp.editData(player.getInstance(), targetBlockPosition, block,
                        data -> updater.accept(data.actions()));
            } else if (block.handler() instanceof StatusPlateBlock sp) {
                sp.editData(player.getInstance(), targetBlockPosition, block,
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
            player.sendMessage(MapMessages.COMMAND_SETPRECISECOORDS_SUCCESS.with(CoordinateUtil.asTranslationArgs(pos)));
        } else {
            player.sendMessage(Component.translatable("create_maps.checkpoint.teleport.too_low"));
        }

    }

}
