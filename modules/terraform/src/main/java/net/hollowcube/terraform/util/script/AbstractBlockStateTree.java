package net.hollowcube.terraform.util.script;

import net.hollowcube.terraform.mask.script.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public abstract class AbstractBlockStateTree<T> implements ParseTree<T> {

    protected final int start;
    protected final int end;
    // Both -1 if not present
    protected final int openBracket;
    protected final int closeBracket;
    // Block name, eg `sto`
    protected final String block;
    // Block properties, eg `foo=bar`
    // Null if openBracket == -1, otherwise never null.
    protected final List<Property> props;

    protected AbstractBlockStateTree(
            int start, int end, int openBracket, int closeBracket,
            @NotNull String block, @UnknownNullability List<Property> props
    ) {
        this.start = start;
        this.end = end;
        this.openBracket = openBracket;
        this.closeBracket = closeBracket;
        this.block = block;
        this.props = props;
    }

    @Override
    public int start() {
        return start;
    }

    @Override
    public int end() {
        return end;
    }

    public record Property(
            int start, int end,
            String key,
            @Nullable String value
    ) implements Tree {

    }
}
