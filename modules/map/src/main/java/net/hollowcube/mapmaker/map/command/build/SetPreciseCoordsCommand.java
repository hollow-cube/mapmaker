package net.hollowcube.mapmaker.map.command.build;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.block.custom.CheckpointPlateBlock;
import net.hollowcube.mapmaker.map.block.custom.StatusPlateBlock;
import net.hollowcube.mapmaker.map.feature.play.effect.BaseEffectData;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            player.sendMessage("You must be editing a checkpoint to use this command. todo this message");
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

        //todo update message
        player.sendMessage(MapMessages.COMMAND_SETSPAWN_SUCCESS.with(
                Component.text(pos.blockX()).hoverEvent(Component.text(pos.x(), NamedTextColor.WHITE)),
                Component.text(pos.blockY()).hoverEvent(Component.text(pos.y(), NamedTextColor.WHITE)),
                Component.text(pos.blockZ()).hoverEvent(Component.text(pos.z(), NamedTextColor.WHITE)),
                Component.text(Math.floor(pos.pitch())).hoverEvent(Component.text(pos.pitch(), NamedTextColor.WHITE)),
                Component.text(Math.floor(pos.yaw())).hoverEvent(Component.text(pos.yaw(), NamedTextColor.WHITE))
        ));
    }

}
