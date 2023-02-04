package net.hollowcube.terraform.action;

import net.hollowcube.terraform.action.edit.WorldView;
import net.hollowcube.terraform.history.Change;
import net.hollowcube.terraform.instance.SchemBlockBatch;
import net.hollowcube.terraform.instance.Schematic;
import net.hollowcube.terraform.instance.SchematicBuilder;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class ActionBuilder {
    private final LocalSession session;

    private Iterable<Point> source;
    private Point at;
    private Block block;

    private Mask mask = null;

    public ActionBuilder(@NotNull LocalSession session) {
        this.session = session;
    }

    // Sources

    public @NotNull ActionBuilder at(@NotNull Point point) {
        this.at = point;
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull ActionBuilder from(@NotNull Iterable<Point> source) {
        this.source = source;
        return this;
    }

    // Masks

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull ActionBuilder matching(@NotNull BiPredicate<Point, Block> predicate) {
        this.mask = (world, point, block) -> predicate.test(point, block);
        return this;
    }

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull ActionBuilder mask(@NotNull Mask mask) {
        this.mask = mask;
        return this;
    }


    // Actions

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull ActionBuilder set(@NotNull Block block) {
        this.block = block;
        return this;
    }


    // Executions

    public void toSchematic(@NotNull Consumer<Schematic> callback) {
        Check.notNull(source, "Source must be set");

        var builder = new SchematicBuilder();
        for (var point : source) {
            builder.addBlock(point, session.instance().getBlock(point));
        }

        builder.setOffset(at.mul(-1));
        callback.accept(builder.toSchematic());
    }

    public void execute(@NotNull Consumer<ActionSummary> callback) {
        Check.notNull(source, "Source must be set");
        Check.notNull(block, "No action specified");

        WorldView world = WorldView.snapshot(session.instance());
        ForkJoinPool.commonPool().submit(() -> executeInternal(session.instance(), world, callback));
    }

    private void executeInternal(@NotNull Instance realInstance, @NotNull WorldView world, @NotNull Consumer<ActionSummary> callback) {
        int i = 0;
        var applyBatch = new SchemBlockBatch();
        for (var point : source) {
            if (mask != null && !mask.test(world, point, world.getBlock(point))) {
                continue;
            }
            applyBatch.setBlock(point, block);
            i++;
        }

        var updates = i;
        //todo chunk batch should lock the chunk to do its work, the advantage is that we process different chunks in different threads.
        var redoSchematic = applyBatch.getSchematic();
        applyBatch.apply(realInstance)
                .thenAccept(undoSchematic -> {
                    session.remember(Change.of(undoSchematic, redoSchematic));
                    callback.accept(new ActionSummary(updates));
                });
    }

}
