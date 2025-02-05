package net.hollowcube.terraform.compat.worldedit.command;

import com.mojang.datafixers.util.Function3;
import it.unimi.dsi.fastutil.ints.Int2IntSortedMap;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.terraform.compat.worldedit.command.arg.WEArgument;
import net.hollowcube.terraform.compat.worldedit.util.WECommand;
import net.hollowcube.terraform.compat.worldedit.util.WEMessages;
import net.hollowcube.terraform.compute.UtilityFunctions;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.selection.Selection;
import net.hollowcube.terraform.selection.region.Region;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.terraform.util.MessageSet;
import net.hollowcube.terraform.util.Messages;
import net.hollowcube.terraform.util.math.CoordinateUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public final class SelectionCommands {

    public static class Pos extends WECommand {

        private final Argument<Vec> coordinatesArg = WEArgument.CommaSeparatedVec3("coordinates");
        private final Argument<Point> positionArg = Argument.RelativeVec3("position");

        private final Messages alreadySetMessage;
        private final Function3<Selection, Point, Boolean, Boolean> setter;

        private Pos(@NotNull String command, @NotNull Messages alreadySetMessage, Function3<Selection, Point, Boolean, Boolean> setter) {
            super(command);

            this.alreadySetMessage = alreadySetMessage;
            this.setter = setter;

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), positionArg);
            addSyntax(playerOnly(this::execute), coordinatesArg);
        }

        public static Pos Pos1() {
            return new Pos("/pos1", Messages.SELECTION_POS1_ALREADY_SET, Selection::selectPrimary);
        }

        public static Pos Pos2() {
            return new Pos("/pos2", Messages.SELECTION_POS2_ALREADY_SET, Selection::selectSecondary);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            Point coordinates = context.get(coordinatesArg);
            if (coordinates == null) coordinates = context.get(positionArg);
            if (coordinates == null) coordinates = player.getPosition();

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            if (!this.setter.apply(selection, coordinates, true)) {
                player.sendMessage(this.alreadySetMessage);
            }
        }
    }

    public static class HPos1 extends WECommand {
        public HPos1() {
            super("/hpos1");

            addSyntax(playerOnly(this::execute));
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            // Determine the target position
            var targetBlock = PlayerUtil.getTargetBlock(player, 512, true);
            if (targetBlock == null) {
                player.sendMessage(Messages.SELECTION_HPOS_NO_BLOCK);
                return;
            }

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            var changed = selection.selectPrimary(targetBlock, true);
            if (!changed) {
                player.sendMessage(Messages.SELECTION_POS1_ALREADY_SET);
            }
        }
    }

    public static class HPos2 extends WECommand {
        public HPos2() {
            super("/hpos2");

            addSyntax(playerOnly(this::execute));
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            // Determine the target position
            var targetBlock = PlayerUtil.getTargetBlock(player, 512, true);
            if (targetBlock == null) {
                player.sendMessage(Messages.SELECTION_HPOS_NO_BLOCK);
                return;
            }

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            var changed = selection.selectSecondary(targetBlock, true);
            if (!changed) {
                player.sendMessage(Messages.SELECTION_POS2_ALREADY_SET);
            }
        }
    }

    public static class Chunk extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Vec> coordinatesArg = WEArgument.CommaSeparatedVec3("coordinates");

        private enum Flags {
            SELECTION,
            CHUNK_COORDINATES
        }

        public Chunk() {
            super("/chunk");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), flagsArg);
            addSyntax(playerOnly(this::execute), coordinatesArg);
            addSyntax(playerOnly(this::execute), flagsArg, coordinatesArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            // For compatibility with worldedit, this command works on 16x256x16 chunks.

            var flags = context.get(flagsArg);
            var coordinates = context.get(coordinatesArg);

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            // Selection is handled significantly differently to just expand from the min and max
            if (flags.contains(Flags.SELECTION)) {
                expandSelectionToChunk(player, selection);
                return;
            }

            // The target chunk index is the coordinates if provided (in chunk or world coordinates depending
            // on whether the -c flag is set). Otherwise, its the current chunk of the player.
            Point chunkIndex;
            if (coordinates != null) {
                if (flags.contains(Flags.CHUNK_COORDINATES)) {
                    chunkIndex = coordinates;
                } else {
                    chunkIndex = chunkIndex(coordinates);
                }
            } else {
                chunkIndex = chunkIndex(player.getPosition());
            }

            // Replace the selection with the current chunk
            selection.setType(Region.Type.CUBOID);
            selection.selectPrimary(blockPos(chunkIndex), false);
            selection.selectSecondary(blockPos(chunkIndex.add(1)).sub(1), false);
            player.sendMessage(WEMessages.SELECTION_CHUNK.with(
                    CoordinateUtil.prettyBlockPos(chunkIndex)
            ));
        }

        private void expandSelectionToChunk(@NotNull Player player, Selection selection) {
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var minChunk = chunkIndex(region.min());
            var maxChunk = chunkIndex(region.max());

            selection.setType(Region.Type.CUBOID);
            selection.selectPrimary(blockPos(minChunk), false);
            selection.selectSecondary(blockPos(maxChunk).sub(1), false);
            player.sendMessage(WEMessages.SELECTION_CHUNK_RANGE.with(
                    CoordinateUtil.prettyBlockPos(minChunk),
                    CoordinateUtil.prettyBlockPos(maxChunk)
            ));
        }

        private @NotNull Point chunkIndex(@NotNull Point point) {
            return new Vec(point.blockX() >> 4, point.blockY() >> 8, point.blockZ() >> 4);
        }

        private @NotNull Point blockPos(@NotNull Point point) {
            return new Vec(point.blockX() << 4, point.blockY() << 8, point.blockZ() << 4);
        }
    }

    public static class Wand extends WECommand {
        public Wand() {
            super("/wand");

            addSyntax(playerOnly(this::execute));
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var tf = LocalSession.forPlayer(player).terraform();
            var itemStack = tf.toolHandler().createBuiltinTool("terraform:wand");

            PlayerUtil.giveItem(player, itemStack);
            player.sendMessage(Messages.TOOL_CREATED.with(Component.text("wand")));
        }
    }

    public static class Expand extends WECommand {
        private final Argument<String> vertArg = Argument.Literal("vert");
        private final Argument<Integer> amountArg = Argument.Int("amount");
        private final Argument<Integer> reverseAmountArg = Argument.Int("reverseAmount").defaultValue(0);
        private final Argument<Direction> directionArg = WEArgument.Direction("direction");

        public Expand() {
            super("/expand");

            addSyntax(playerOnly(this::execute), vertArg);
            addSyntax(playerOnly(this::execute), amountArg);
            addSyntax(playerOnly(this::execute), amountArg, reverseAmountArg);
            addSyntax(playerOnly(this::execute), amountArg, directionArg);
            addSyntax(playerOnly(this::execute), amountArg, reverseAmountArg, directionArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var isVertical = context.has(vertArg);
            var amount = context.get(amountArg);
            var reverseAmount = context.get(reverseAmountArg);
            var direction = context.get(directionArg);

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var dimensionType = session.instance().getCachedDimensionType();
            int dimensionMin = dimensionType.minY(), dimensionMax = dimensionType.maxY();

            // Make the relevant adjustments based on the given args
            Point low, high;
            if (isVertical) {
                low = new Vec(0, -(region.min().blockY() - dimensionMin), 0);
                high = new Vec(0, region.max().blockY() - dimensionMin, 0);
            } else {
                var normal = new Vec(direction.normalX(), direction.normalY(), direction.normalZ());
                if (normal.x() > 0 || normal.y() > 0 || normal.z() > 0) {
                    low = normal.mul(-reverseAmount);
                    high = normal.mul(amount);
                } else {
                    low = normal.mul(-amount);
                    high = normal.mul(reverseAmount);
                }
            }

            // Update the region
            try {
                selection.reshape(low, high);

                var deltaVolume = selection.region().volume() - region.volume();
                player.sendMessage(WEMessages.SELECTION_EXPANDED.with(deltaVolume));
            } catch (UnsupportedOperationException e) {
                player.sendMessage(Messages.SELECTION_RESHAPE_UNSUPPORTED.with(selection.type()));
            }
        }
    }

    public static class Contract extends WECommand {
        public Contract() {
            super("/contract");
        }
    }

    private static abstract class BaseUniformReshape extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Integer> amountArg = Argument.Int("amount");

        private enum Flags {
            HORIZONTAL,
            VERTICAL
        }

        public BaseUniformReshape(@NotNull String name) {
            super(name);

            addSyntax(playerOnly(this::execute), amountArg);
            addSyntax(playerOnly(this::execute), flagsArg, amountArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var amount = Math.abs(context.get(amountArg)); // Worldedit seems to do an abs here, not sure why.

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            // The behavior here is intentionally that we end up with a zero vector if both flags are set.
            // This matches the behavior observed in worldedit.
            var normal = new Vec(
                    !flags.contains(Flags.VERTICAL) ? 1 : 0,
                    !flags.contains(Flags.HORIZONTAL) ? 1 : 0,
                    !flags.contains(Flags.VERTICAL) ? 1 : 0
            );

            // Update the region
            try {
                selection.reshape(normal.mul(-amount * modifier()), normal.mul(amount * modifier()));

                var deltaVolume = Math.abs(selection.region().volume() - region.volume());
                player.sendMessage(completedMessage().with(deltaVolume));
            } catch (UnsupportedOperationException e) {
                player.sendMessage(Messages.SELECTION_RESHAPE_UNSUPPORTED.with(selection.type()));
            }
        }

        protected abstract int modifier();

        protected abstract @NotNull MessageSet completedMessage();
    }

    public static class Outset extends BaseUniformReshape {

        public Outset() {
            super("/outset");
        }

        @Override
        protected int modifier() {
            return 1;
        }

        @Override
        protected @NotNull MessageSet completedMessage() {
            return WEMessages.SELECTION_OUTSET;
        }
    }

    public static class Inset extends BaseUniformReshape {
        public Inset() {
            super("/inset");
        }

        @Override
        protected int modifier() {
            return -1;
        }

        @Override
        protected @NotNull MessageSet completedMessage() {
            return WEMessages.SELECTION_INSET;
        }
    }

    public static class Shift extends WECommand {
        private final Argument<Integer> amountArg = Argument.Int("amount");
        private final Argument<Direction> directionArg = WEArgument.Direction("direction");

        public Shift() {
            super("/shift");

            addSyntax(playerOnly(this::execute), amountArg);
            addSyntax(playerOnly(this::execute), amountArg, directionArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var amount = context.get(amountArg);
            var direction = context.get(directionArg);

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            // Update the region
            try {
                var normal = new Vec(direction.normalX(), direction.normalY(), direction.normalZ());
                selection.reshape(normal.mul(amount), normal.mul(amount));

                player.sendMessage(WEMessages.SELECTION_SHIFTED.with(amount));
            } catch (UnsupportedOperationException e) {
                player.sendMessage(Messages.SELECTION_RESHAPE_UNSUPPORTED.with(selection.type()));
            }
        }
    }

    public static class Size extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);

        private enum Flags {
            CLIPBOARD
        }

        public Size() {
            super("/size");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), flagsArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);

            var session = LocalSession.forPlayer(player);
            if (flags.contains(Flags.CLIPBOARD)) {
                showClipboardInfo(player, session);
                return;
            }

            var selection = session.selection(Selection.DEFAULT);
            var region = selection.region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            player.sendMessage("todo send size info of current selection");
            //todo need some kind of selection.describe()
        }

        private void showClipboardInfo(@NotNull Player player, @NotNull LocalSession session) {
            player.sendMessage("todo send clipboard infos");
        }
    }

    public static class Count extends WECommand {
        private final Argument<Mask> maskArg = WEArgument.Mask("mask").defaultValue(Mask.not(Mask.air()));

        public Count() {
            super("/count");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), maskArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var mask = context.get(maskArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var counter = UtilityFunctions.counting(region, Mask.not(mask), false, false);

            // This is kind of a hack to ensure that the count is queued like any other operation.
            // Basically we schedule a BS task which does the expected counting and then returns an empty buffer.
            session.buildTask("we-count")
                    .compute(counter)
                    .post(result -> player.sendMessage(WEMessages.SELECTION_COUNT.with(counter.total())))
                    .ephemeral()
                    .dryRun();
        }
    }

    public static class Distr extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Integer> pageArg = Argument.Int("page").defaultValue(1);

        private enum Flags {
            CLIPBOARD,
            DISPLAY_BY_STATE,
            PAGE
        }

        public Distr() {
            super("/distr");

            //todo need to handle page arg + store last result for future use.

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), flagsArg);
            addSyntax(playerOnly(this::execute), flagsArg, pageArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var page = context.get(pageArg);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var counter = UtilityFunctions.counting(region, Mask.never(),
                    !flags.contains(Flags.DISPLAY_BY_STATE), flags.contains(Flags.DISPLAY_BY_STATE));

            // This is kind of a hack to ensure that the count is queued like any other operation.
            // Basically we schedule a BS task which does the expected counting and then returns an empty buffer.
            session.buildTask("we-distr")
                    .compute(counter)
                    .post(result -> {
                        //todo translation keys
                        Int2IntSortedMap counts = flags.contains(Flags.DISPLAY_BY_STATE)
                                ? counter.countsByState() : counter.countsByBlock();

                        player.sendMessage("total block count: " + counter.total());
                        for (var entry : counts.int2IntEntrySet()) {
                            var block = flags.contains(Flags.DISPLAY_BY_STATE)
                                    ? Block.fromStateId((short) entry.getIntKey())
                                    : Block.fromBlockId(entry.getIntKey());
                            assert block != null;

                            player.sendMessage(String.format("%.3f%% %d %s",
                                    (entry.getIntValue() / (float) counter.total()) * 100,
                                    entry.getIntValue(),
                                    block.name()));
                        }
                    })
                    .ephemeral()
                    .dryRun();
        }
    }

    public static class Sel extends WECommand {
        private final Argument<String> selectorArg = Argument.Word("selector");

        public Sel() {
            super("/sel");

            addSyntax(playerOnly(this::executeClear));
            addSyntax(playerOnly(this::executeChangeSelector), selectorArg);
        }

        private void executeClear(@NotNull Player player, @NotNull CommandContext context) {
            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            selection.clear();
            player.sendMessage(Messages.SELECTION_CLEARED);
        }

        private void executeChangeSelector(@NotNull Player player, @NotNull CommandContext context) {
            //todo implement
            player.sendMessage("not yet implemented.");
        }
    }

    private SelectionCommands() {
    }
}
