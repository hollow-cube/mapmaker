package net.hollowcube.terraform.compat.vanilla.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class SetBlockCommand extends CommandDsl {

    private final Argument<Point> posArg = Argument.RelativeVec3("pos");
    private final Argument<Block> blockArg = Argument.Block("block");
    private final Argument<Mode> modeArg = Argument.Enum("mode", Mode.class).defaultValue(Mode.DESTROY);

    public SetBlockCommand() {
        super("setblock");

        addSyntax(playerOnly(this::execute), posArg, blockArg);
        addSyntax(playerOnly(this::execute), posArg, blockArg, modeArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var point = context.get(posArg);
        var block = context.get(blockArg);
        var mode = context.get(modeArg);

        var session = LocalSession.forPlayer(player);
        session.buildTask("vanilla-setblock")
                .metadata()
                .compute((task, world) -> {
                    var buffer = BlockBuffer.builder(world, point, point);
                    var worldBlock = world.getBlock(point);
                    var canPlace = switch (mode) {
                        case KEEP -> worldBlock.isAir();
                        case REPLACE -> !worldBlock.compare(block);
                        case null, default -> true;
                    };
                    if (!canPlace) return buffer.build();

                    buffer.set(point, block);

                    return buffer.build();
                })
                .ephemeral()
                .submit();
    }

    private enum Mode {
        DESTROY, // default
        KEEP,
        REPLACE,
        STRICT // also default, (doesnt propagate block updates)
    }
}
