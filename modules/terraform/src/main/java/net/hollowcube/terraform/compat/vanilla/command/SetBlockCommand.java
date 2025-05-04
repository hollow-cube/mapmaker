package net.hollowcube.terraform.compat.vanilla.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.command.util.CommandPreviewHelper;
import net.hollowcube.terraform.cui.ClientRenderer;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.Messages;
import net.minestom.server.coordinate.BlockVec;
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

        addSuggestionSyntax(playerOnly(this::suggest), posArg);
        addSyntax(playerOnly(this::execute), posArg, blockArg);
        addSyntax(playerOnly(this::execute), posArg, blockArg, modeArg);
    }

    private void suggest(@NotNull Player player, @NotNull CommandContext context) {
        if (!context.has(posArg)) {
            return;
        }

        Point p1 = new BlockVec(context.get(posArg));

        var session = LocalSession.forPlayer(player);
        var renderer = session.cui().renderer();
        renderer.switchTo(ClientRenderer.RenderContext.COMMAND, true);
        CommandPreviewHelper.debounceContext(player, renderer);
        renderer.begin("setblock");
        session.cui().renderer().cuboid(
                p1,
                p1.add(1,1,1),
                ClientRenderer.RenderType.PRIMARY);
        renderer.end("setblock");
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var point = context.get(posArg);
        var block = context.get(blockArg);
        var mode = context.get(modeArg);

        var session = LocalSession.forPlayer(player);
        session.cui().renderer().switchTo(ClientRenderer.RenderContext.NORMAL, false);
        session.buildTask("vanilla-setblock")
                .metadata()
                .compute((_, world) -> {
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
                .post(result -> player.sendMessage(Messages.GENERIC_BLOCKS_CHANGED.with(result.blocksChanged())))
                .submit();
    }

    private enum Mode {
        DESTROY, // default
        KEEP,
        REPLACE,
        STRICT // also default, (doesnt propagate block updates)
    }
}
