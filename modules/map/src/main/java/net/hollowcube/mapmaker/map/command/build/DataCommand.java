package net.hollowcube.mapmaker.map.command.build;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.TagStringIO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class DataCommand extends CommandDsl {
    private static final Argument<Point> blockPositionArg = Argument.RelativeVec3("position");
    private static final Argument<CompoundBinaryTag> nbtArg = Argument.GreedyString("nbt")
            .map((ignored, input) -> {
                try {
                    return new ParseResult.Success<>(TagStringIO.get().asCompound(input));
                } catch (Exception e) {
                    return new ParseResult.Partial<>();
                }
            });

    public final Get get;
    public final Merge merge;

    public DataCommand() {
        super("data");

        description = "Modify block or entity data";

        setCondition(mapFilter(false, true, false));

        addSubcommand(this.get = new Get());
        addSubcommand(this.merge = new Merge());
    }

    private static class Get extends CommandDsl {

        public Get() {
            super("get");

            addSyntax(playerOnly(this::handleGetBlockData), Argument.Literal("block"), blockPositionArg);
        }

        private void handleGetBlockData(@NotNull Player player, @NotNull CommandContext context) {
            var blockPosition = context.get(blockPositionArg);

            var instance = player.getInstance();
            var block = instance.getBlock(blockPosition, Block.Getter.Condition.NONE);

            var nbtData = block.nbt();
            if (nbtData == null) {
                player.sendMessage("Block has no data: " + blockPosition);
                return;
            }

            try {
                player.sendMessage(Component.text("Block data: ").append(NbtUtil.prettyPrint(nbtData)
                        .hoverEvent(Component.text("Click to copy SNBT", NamedTextColor.WHITE))
                        .clickEvent(ClickEvent.copyToClipboard(TagStringIO.get().asString(nbtData)))));
            } catch (IOException e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        }
    }

    private static class Merge extends CommandDsl {

        public Merge() {
            super("merge");

            addSyntax(playerOnly(this::handleMergeBlockData), Argument.Literal("block"), blockPositionArg, nbtArg);
        }

        private void handleMergeBlockData(@NotNull Player player, @NotNull CommandContext context) {
            var blockPosition = context.get(blockPositionArg);
            var nbt = context.get(nbtArg);

            var instance = player.getInstance();
            var block = instance.getBlock(blockPosition, Block.Getter.Condition.NONE);

            var nbtData = block.nbt();
            if (nbtData == null) {
                player.sendMessage("Block has no data: " + blockPosition);
                return;
            }

            var newNbtData = NbtUtil.deepMerge(nbtData, nbt);
            instance.setBlock(blockPosition, block.withNbt(newNbtData));
            player.sendMessage("Block data updated: " + blockPosition);
        }
    }

}
