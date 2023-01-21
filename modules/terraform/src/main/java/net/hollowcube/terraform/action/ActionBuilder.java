package net.hollowcube.terraform.action;

import net.hollowcube.terraform.session.LocalSession;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.batch.AbsoluteBlockBatch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ActionBuilder {
    private final LocalSession session;

    private Iterable<Point> source;
    private Block block;

    public ActionBuilder(@NotNull LocalSession session) {
        this.session = session;
    }

    // Sources

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull ActionBuilder from(@NotNull Iterable<Point> source) {
        this.source = source;
        return this;
    }


    //todo masks


    // Actions

    @Contract(value = "_ -> this", mutates = "this")
    public @NotNull ActionBuilder set(@NotNull Block block) {
        this.block = block;
        return this;
    }


    // Executions

    public void execute(@NotNull Consumer<ActionSummary> callback) {
        Check.notNull(source, "Source must be set");
        Check.notNull(block, "No action specified");

        int i = 0;
        var applyBatch = new AbsoluteBlockBatch();
        for (var point : source) {
            applyBatch.setBlock(point, block);
            i++;
        }

        applyBatch.apply(session.instance(), null);
        callback.accept(new ActionSummary(i));
    }

}
