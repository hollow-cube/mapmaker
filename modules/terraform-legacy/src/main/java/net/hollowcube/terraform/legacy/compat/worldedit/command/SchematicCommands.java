package net.hollowcube.terraform.legacy.compat.worldedit.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.schem.util.Rotation;
import net.hollowcube.terraform.apply.CaptureEngine;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.legacy.buffer.EditBlockBuffer;
import net.hollowcube.terraform.legacy.command.util.ArgumentRotation;
import net.hollowcube.terraform.legacy.command.util.CommandPreviewHelper;
import net.hollowcube.terraform.legacy.compat.worldedit.command.arg.WEArgument;
import net.hollowcube.terraform.legacy.compat.worldedit.util.WECommand;
import net.hollowcube.terraform.legacy.compat.worldedit.util.WEMessages;
import net.hollowcube.terraform.legacy.compute.RegionFunctions;
import net.hollowcube.terraform.legacy.cui.ClientRenderer;
import net.hollowcube.terraform.legacy.mask.Mask;
import net.hollowcube.terraform.legacy.pattern.Pattern;
import net.hollowcube.terraform.legacy.schem.Schematics;
import net.hollowcube.terraform.legacy.selection.Selection;
import net.hollowcube.terraform.legacy.selection.region.CuboidRegion;
import net.hollowcube.terraform.legacy.selection.region.Region;
import net.hollowcube.terraform.legacy.session.Clipboard;
import net.hollowcube.terraform.legacy.session.LocalSession;
import net.hollowcube.terraform.legacy.session.PlayerSession;
import net.hollowcube.terraform.legacy.task.Task;
import net.hollowcube.terraform.legacy.util.Format;
import net.hollowcube.terraform.legacy.util.Messages;
import net.hollowcube.terraform.util.Transform;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class SchematicCommands {

    public static class Schem extends WECommand {
        public Schem() {
            super("/schem");

            addSubcommand(new List());
            addSubcommand(new Formats());
            addSubcommand(new Load());
            addSubcommand(new Delete());
            addSubcommand(new Save());
        }

        public static class List extends WECommand {
            //todo correctly implementing the flag filtering on this command requires reworking how WE flags are implemented.
            // will do this eventually, but its a bit non-trivial. Basically worldedit has the following syntax:
            // /schem list [-dn] [-p <page>] [-f <format>] [filter]
            // but you can provide the flags in any order and in multiple groups. The following are all valid:
            // /schem list -d -n -p 2 -f sponge
            // /schem list -p 2 -f sponge -d -n
            // /schem list -dp 2 -nf sponge

            private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
            private final Argument<Integer> pageArg = Argument.Int("page").min(1).defaultValue(1);
            private final Argument<String> filterArg = Argument.Word("filter");
            //todo support format arg

            private enum Flags {
                PAGE,
                D_SORT_OLDEST,
                D_SORT_NEWEST,
                FORMAT,
            }

            public List() {
                super("list");

                addSyntax(playerOnly(this::execute));
                addSyntax(playerOnly(this::execute), flagsArg);
                addSyntax(playerOnly(this::execute), flagsArg, pageArg);
                addSyntax(playerOnly(this::execute), flagsArg, pageArg, filterArg);
                addSyntax(playerOnly(this::execute), flagsArg, filterArg);
                addSyntax(playerOnly(this::execute), filterArg);
            }

            private void execute(@NotNull Player player, @NotNull CommandContext context) {
                var flags = context.get(flagsArg);
                var page = context.get(pageArg);
                var filter = context.get(filterArg);

                //todo finish impl + update backend to store format type and whatever else.

                var session = PlayerSession.forPlayer(player);
                var schematics = session.terraform().storage().listSchematics(session.id());
                schematics.forEach(s -> player.sendMessage(s.name() + " " + s.dimensions() + " " + Format.formatBytes(s.size())));
            }

        }

        public static class Formats extends WECommand {
            public Formats() {
                super("formats");

                addSyntax(playerOnly(this::execute));
            }

            private void execute(@NotNull Player player, @NotNull CommandContext context) {
                player.sendMessage("currently only sponge schematic v1,v2,v3 (.schem) are supported.");
            }

        }

        public static class Load extends WECommand {
            private final Argument<String> nameArg = Argument.Word("name");

            public Load() {
                super("load");

                addSyntax(playerOnly(this::execute), nameArg);
            }

            private void execute(@NotNull Player player, @NotNull CommandContext context) {
                var name = context.get(nameArg);

                var session = PlayerSession.forPlayer(player);
                var clipboard = session.clipboard(Clipboard.DEFAULT);

                var storage = session.terraform().storage();
                var schemData = storage.loadSchematicData(session.id(), name);
                if (schemData == null) {
                    player.sendMessage(Messages.SCHEM_NOT_FOUND.with(name));
                    return;
                }

                try {
                    clipboard.setData(new net.hollowcube.terraform.clipboard.Clipboard(Schematics.read(schemData)));
                } catch (IOException e) {
                    throw new RuntimeException("failed to read schematic " + name, e);
                }
                player.sendMessage(Messages.SCHEM_LOADED.with(name));
            }
        }

        public static class Delete extends WECommand {
            private final Argument<String> nameArg = Argument.Word("name");

            public Delete() {
                super("delete");

                addSyntax(playerOnly(this::execute), nameArg);
            }

            private void execute(@NotNull Player player, @NotNull CommandContext context) {
                var name = context.get(nameArg);

                var session = PlayerSession.forPlayer(player);
                var storage = session.terraform().storage();
                var result = storage.deleteSchematic(session.id(), name);
                player.sendMessage(switch (result) {
                    case SUCCESS -> Messages.SCHEM_DELETED.with(name);
                    case NOT_FOUND -> Messages.SCHEM_NOT_FOUND.with(name);
                });
            }
        }

        public static class Save extends WECommand {
            private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
            private final Argument<String> nameArg = Argument.Word("name");

            private enum Flags {
                FORCE,
            }

            public Save() {
                super("save");

                addSyntax(playerOnly(this::execute), nameArg);
                addSyntax(playerOnly(this::execute), flagsArg, nameArg);
            }

            private void execute(@NotNull Player player, @NotNull CommandContext context) {
                var flags = context.get(flagsArg);
                var name = context.get(nameArg);

                var session = PlayerSession.forPlayer(player);
                var data = session.clipboard(Clipboard.DEFAULT).data();
                if (data == null) {
                    player.sendMessage(Messages.GENERIC_NO_CLIPBOARD);
                    return;
                }

                var schematic = data.toSchematic();
                byte[] bytes;
                try {
                    bytes = Schematics.write(schematic);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Messages.SCHEM_SIZE_LIMIT_EXCEEDED);
                    return;
                }

                var bounds = Objects.requireNonNull(schematic.buffer().blockBounds());
                var size = bounds.max().sub(bounds.min()).add(1);
                var storage = session.terraform().storage();
                var result = storage.createSchematic(session.id(), name, bytes, size, flags.contains(Flags.FORCE));

                player.sendMessage(switch (result) {
                    case SUCCESS -> Messages.SCHEM_SAVED.with(name);
                    case DUPLICATE_ENTRY -> Messages.SCHEM_DUPLICATE.with(name);
                    case ENTRY_LIMIT_EXCEEDED -> Messages.SCHEM_LIMIT_EXCEEDED;
                    case SIZE_LIMIT_EXCEEDED -> Messages.SCHEM_SIZE_LIMIT_EXCEEDED;
                });
            }
        }
    }

    public static class Copy extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Mask> maskArg = WEArgument.Mask("mask");

        private enum Flags {
            ENTITIES,
            BIOMES,
            CENTER,
            MASK,
        }

        public Copy() {
            super("/copy");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), flagsArg);
            addSyntax(playerOnly(this::execute), flagsArg, maskArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var mask = context.get(maskArg);

            if (flags.contains(Flags.MASK)) {
                //todo idk what exception this should be
                Check.argCondition(mask == null, "Mask is required when using the mask flag");
            } else mask = Mask.always();

            // Warnings for currently unsupported flags
            if (flags.contains(Flags.ENTITIES))
                player.sendMessage(Messages.GENERIC_ENTITIES_UNSUPPORTED);
            if (flags.contains(Flags.BIOMES))
                player.sendMessage(Messages.GENERIC_BIOMES_UNSUPPORTED);

            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            Point anchor = flags.contains(Flags.CENTER)
                    ? region.min().add(region.max()).div(2)
                    : player.getPosition();
            Task task;
            if (!flags.contains(Flags.MASK) && region instanceof CuboidRegion) {
                // Captures the box straight from the world, a palette copy per whole section, and applies nothing.
                var instance = session.instance();
                var box = Area.cuboid(region.min(), region.max().sub(1, 1, 1));
                var copied = new AtomicReference<EditBuffer>();
                task = session.buildTask("we-copy")
                        .metadata() //todo
                        .compute((_, _) -> {
                            copied.set(CaptureEngine.capture(instance, box));
                            return new EditBlockBuffer(EditBuffer.empty());
                        })
                        .post(_ -> {
                            clipboard.setData(new net.hollowcube.terraform.clipboard.Clipboard(copied.get(), anchor));
                            player.sendMessage(Messages.CLIPBOARD_COPY.with(region.volume()));
                        })
                        .dryRunIfCapacity();
            } else {
                task = session.buildTask("we-copy")
                        .metadata() //todo
                        .compute(RegionFunctions.replace(region, mask, Pattern.air()))
                        .post(result -> {
                            var copied = EditBlockBuffer.toEditBuffer(result.undoBuffer());
                            clipboard.setData(new net.hollowcube.terraform.clipboard.Clipboard(copied, anchor));
                            player.sendMessage(Messages.CLIPBOARD_COPY.with(result.blocksChanged()));
                        })
                        .dryRunIfCapacity();
            }
            if (task == null) {
                player.sendMessage(Messages.GENERIC_QUEUE_FULL);
            }
        }
    }

    public static class Cut extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Pattern> patternArg = WEArgument.Pattern("pattern").defaultValue(Pattern.air());
        private final Argument<Mask> maskArg = WEArgument.Mask("mask");

        private enum Flags {
            ENTITIES,
            BIOMES,
            MASK,
        }

        public Cut() {
            super("/cut");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), flagsArg);
            addSyntax(playerOnly(this::execute), flagsArg, maskArg);
            addSyntax(playerOnly(this::execute), patternArg, flagsArg);
            addSyntax(playerOnly(this::execute), patternArg, flagsArg, maskArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            var pattern = context.get(patternArg);
            var mask = context.get(maskArg);

            if (flags.contains(Flags.MASK)) {
                player.sendMessage("Mask is required when using the mask flag");
                return;
            } else mask = Mask.always();

            // Warnings for currently unsupported flags
            if (flags.contains(Flags.ENTITIES))
                player.sendMessage(Messages.GENERIC_ENTITIES_UNSUPPORTED);
            if (flags.contains(Flags.BIOMES))
                player.sendMessage(Messages.GENERIC_BIOMES_UNSUPPORTED);

            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

            var session = LocalSession.forPlayer(player);
            var region = session.selection(Selection.DEFAULT).region();
            if (region == null) {
                player.sendMessage(Messages.GENERIC_NO_SELECTION);
                return;
            }

            var origin = player.getPosition();
            Task task;
            if (region instanceof CuboidRegion) {
                // Captures the box straight from the world before clearing it, so the clipboard needs no read of
                // the cells the clear touched.
                var instance = session.instance();
                var box = Area.cuboid(region.min(), region.max().sub(1, 1, 1));
                var clear = RegionFunctions.set(region, pattern);
                var cut = new AtomicReference<EditBuffer>();
                task = session.buildTask("we-cut")
                        .metadata() //todo
                        .compute((computeTask, world) -> {
                            cut.set(CaptureEngine.capture(instance, box));
                            return clear.exec(computeTask, world);
                        })
                        .post(result -> {
                            clipboard.setData(new net.hollowcube.terraform.clipboard.Clipboard(cut.get(), origin));
                            player.sendMessage(Messages.CLIPBOARD_CUT.with(result.blocksChanged()));
                        })
                        .submitIfCapacity();
            } else {
                task = session.buildTask("we-cut")
                        .metadata() //todo
                        .compute(RegionFunctions.replace(region, mask, pattern))
                        .captureAll()
                        .post(result -> {
                            var cut = EditBlockBuffer.toEditBuffer(result.undoBuffer());
                            clipboard.setData(new net.hollowcube.terraform.clipboard.Clipboard(cut, origin));
                            player.sendMessage(Messages.CLIPBOARD_CUT.with(result.blocksChanged()));
                        })
                        .submitIfCapacity();
            }
            if (task == null) {
                player.sendMessage(Messages.GENERIC_QUEUE_FULL);
            }
        }
    }

    public static class Paste extends WECommand {
        private final Argument<EnumSet<Flags>> flagsArg = WEArgument.FlagSet(Flags.class);
        private final Argument<Mask> maskArg = WEArgument.Mask("mask");

        private enum Flags {
            AIR_SKIP,
            ORIGINAL_POS,
            SELECT_REGION,
            NO_PASTE,
            ENTITIES,
            BIOMES,
            MASK
        }

        public Paste() {
            super("/paste");

            addSyntax(playerOnly(this::execute), playerOnly(this::suggest));
            addSyntax(playerOnly(this::execute), flagsArg);
            addSyntax(playerOnly(this::execute), flagsArg, maskArg);
        }

        private void suggest(@NotNull Player player, @NotNull CommandContext context) {
            var playerSession = PlayerSession.forPlayer(player);
            var data = playerSession.clipboard(Clipboard.DEFAULT).data();
            if (data == null) return;
            var bounds = data.pasteBounds(player.getPosition());
            if (bounds == null) return;

            var session = LocalSession.forPlayer(player);
            var min = bounds.min();
            var max = bounds.max();

            var renderer = session.cui().renderer();

            renderer.switchTo(ClientRenderer.RenderContext.COMMAND, true);
            CommandPreviewHelper.debounceContext(player, renderer);
            renderer.begin("paste");
            session.cui().renderer().cuboid(
                    min,
                    max,
                    ClientRenderer.RenderType.PRIMARY);
            renderer.end("paste");
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var flags = context.get(flagsArg);
            if (flags.contains(Flags.MASK)) {
                player.sendMessage("Mask is required when using the mask flag");
                return;
            }

            // Warnings for currently unsupported flags
            if (flags.contains(Flags.ENTITIES))
                player.sendMessage(Messages.GENERIC_ENTITIES_UNSUPPORTED);
            if (flags.contains(Flags.BIOMES))
                player.sendMessage(Messages.GENERIC_BIOMES_UNSUPPORTED);
            if (flags.contains(Flags.ORIGINAL_POS))
                player.sendMessage(Messages.PASTE_ORIGINAL_POS_UNSUPPORTED);

            var playerSession = PlayerSession.forPlayer(player);
            var data = playerSession.clipboard(Clipboard.DEFAULT).data();
            var origin = player.getPosition();
            var bounds = data != null ? data.pasteBounds(origin) : null;
            if (data == null || bounds == null) {
                player.sendMessage(Messages.GENERIC_NO_CLIPBOARD);
                return;
            }

            var session = LocalSession.forPlayer(player);
            var selection = session.selection(Selection.DEFAULT);

            // The pasted box, to select it if that flag is set
            var min = bounds.min();
            var max = bounds.max();
            session.cui().renderer().switchTo(ClientRenderer.RenderContext.NORMAL, false);

            if (flags.contains(Flags.NO_PASTE)) {
                selection.setType(Region.Type.CUBOID);
                selection.selectPrimary(min, false);
                selection.selectSecondary(max, false);
                player.sendMessage(WEMessages.CLIPBOARD_PASTE_SELECT_ONLY);
                return;
            }

            boolean skipAir = flags.contains(Flags.AIR_SKIP);
            var submitted = session.buildTask("we-paste")
                    .metadata() //todo
                    .compute((task, world) -> new EditBlockBuffer(data.pasteBuffer(origin, skipAir)))
                    .post(result -> {
                        if (flags.contains(Flags.SELECT_REGION)) {
                            selection.setType(Region.Type.CUBOID);
                            selection.selectPrimary(min, false);
                            selection.selectSecondary(max, false);
                        }
                        player.sendMessage(Messages.CLIPBOARD_PASTE.with(result.blocksChanged()));
                    })
                    .submitIfCapacity();
            if (submitted == null) {
                player.sendMessage(Messages.GENERIC_QUEUE_FULL);
            }
        }
    }

    public static class Rotate extends WECommand {
        private final Argument<Rotation> yArg = ArgumentRotation.of("rotateY");
        private final Argument<Rotation> xArg = ArgumentRotation.of("rotateX").defaultValue(Rotation.NONE);
        private final Argument<Rotation> zArg = ArgumentRotation.of("rotateZ").defaultValue(Rotation.NONE);

        public Rotate() {
            super("/rotate");

            addSyntax(playerOnly(this::execute), yArg);
            addSyntax(playerOnly(this::execute), yArg, xArg);
            addSyntax(playerOnly(this::execute), yArg, xArg, zArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var xRot = context.get(xArg);
            var yRot = context.get(yArg);
            var zRot = context.get(zArg);

            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);
            if (clipboard.isEmpty()) {
                player.sendMessage(Messages.GENERIC_NO_CLIPBOARD);
                return;
            }

            clipboard.applyTransform(Transform.rotateZ(degrees(zRot))
                    .compose(Transform.rotateY(degrees(yRot)))
                    .compose(Transform.rotateX(degrees(xRot))));
            player.sendMessage(Messages.CLIPBOARD_ROTATED);
        }

        private static int degrees(@NotNull Rotation rotation) {
            return rotation.ordinal() * 90;
        }
    }

    public static class Flip extends WECommand {
        private final Argument<Direction> directionArg = WEArgument.Direction("direction");

        public Flip() {
            super("/flip");

            addSyntax(playerOnly(this::execute));
            addSyntax(playerOnly(this::execute), directionArg);
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var direction = context.get(directionArg);

            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);
            if (clipboard.isEmpty()) {
                player.sendMessage(Messages.GENERIC_NO_CLIPBOARD);
                return;
            }

            clipboard.applyTransform(Transform.mirror(direction.normalX() != 0 ? Transform.Axis.X
                    : direction.normalY() != 0 ? Transform.Axis.Y : Transform.Axis.Z));
            player.sendMessage(Messages.CLIPBOARD_FLIPPED);
        }
    }

    public static class ClearClipboard extends WECommand {
        public ClearClipboard() {
            super("/clearclipboard");

            addSyntax(playerOnly(this::execute));
        }

        private void execute(@NotNull Player player, @NotNull CommandContext context) {
            var playerSession = PlayerSession.forPlayer(player);
            var clipboard = playerSession.clipboard(Clipboard.DEFAULT);

            clipboard.clear();
            player.sendMessage(Messages.CLIPBOARD_CLEARED);
        }
    }

    private SchematicCommands() {
    }
}
