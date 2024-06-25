package net.hollowcube.mapmaker.map.command.build;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class SetPreciseCoordsCommand extends CommandDsl {

    private final Argument<Point> coordArgument = Argument.RelativeVec3("position")
            .description("The position to teleport to");
    private final Argument<Float> yawArgument = Argument.Float("yaw")
            .description("The yaw to teleport to");
    private final Argument<Float> pitchArgument = Argument.Float("pitch")
            .description("The pitch to teleport to");

    @Inject
    public SetPreciseCoordsCommand() {
        super("setprecisecoords", "spc");

        setCondition(mapFilter(false, true, false));

        addSyntax(playerOnly(this::updatePreciseCoords));
        addSyntax(playerOnly(this::updatePreciseCoords), coordArgument);
        addSyntax(playerOnly(this::updatePreciseCoords), coordArgument, yawArgument, pitchArgument);
    }

    private void updatePreciseCoords(@NotNull Player player, @NotNull CommandContext context) {
        // Ensure they are editing a block
        var targetBlockPosition = player.getTag(BaseEffectData.TARGET_PLATE);
        if (targetBlockPosition == null) {
            player.sendMessage(MapMessages.COMMAND_SETPRECISECOORDS_NO_TARGET);
            return;
        }

        // Read the target position from args
        Pos targetPos = player.getPosition();
        if (context.has(coordArgument)) targetPos = targetPos.withCoord(context.get(coordArgument));
        if (context.has(yawArgument)) targetPos = targetPos.withYaw(context.get(yawArgument));
        if (context.has(pitchArgument)) targetPos = targetPos.withPitch(context.get(pitchArgument));

        final Pos pos = targetPos;
        var block = player.getInstance().getBlock(targetBlockPosition);
        if (block.handler() instanceof CheckpointPlateBlock cp) {
            cp.editData(player.getInstance(), targetBlockPosition, block, data -> data.setTeleport(pos));
        } else if (block.handler() instanceof StatusPlateBlock sp) {
            sp.editData(player.getInstance(), targetBlockPosition, block, data -> data.setTeleport(pos));
        } else return;

        player.removeTag(BaseEffectData.TARGET_PLATE);
        player.sendMessage(MapMessages.COMMAND_SETPRECISECOORDS_SUCCESS.with(CoordinateUtil.asTranslationArgs(pos)));
    }

}
