package net.hollowcube.terraform.compat.worldedit.script;

import net.hollowcube.terraform.mask.script.Tree;
import net.hollowcube.terraform.pattern.Pattern;
import net.hollowcube.terraform.util.script.ParseException;
import net.hollowcube.terraform.util.script.ParseTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;

public interface PatternTree extends ParseTree<Pattern> {

    record BlockState(
            @NotNull NamespaceId namespaceId,
            @NotNull PropertyList props
    ) implements PatternTree {
        @Override
        public int start() {
            return namespaceId().start();
        }

        @Override
        public int end() {
            return props().end();
        }

        @Override
        public @NotNull Pattern into() throws ParseException {
            throw new ParseException(start(), end(), "not implemented");
        }
    }

    record LegacyBlock(
            int start, int end,
            int colon, // -1 if not present
            int id,
            int data // -1 if not present
    ) implements PatternTree {
        @Override
        public @NotNull Pattern into() throws ParseException {
            throw new ParseException(start, end, "not implemented");
        }
    }

    record RandomState(
            int start, int end,
            @Nullable NamespaceId namespaceId
    ) implements PatternTree {
        @Override
        public @NotNull Pattern into() throws ParseException {
            throw new ParseException(start, end(), "not implemented");
        }
    }

    record Tag(
            int start, int end,
            @Nullable NamespaceId namespaceId
    ) implements PatternTree {
        @Override
        public @NotNull Pattern into() throws ParseException {
            throw new ParseException(start, end(), "not implemented");
        }
    }

    record WeightedList(
            int start, int end,
            @NotNull List<PatternTree> entries
    ) implements PatternTree {
        @Override
        public @NotNull Pattern into() throws ParseException {
            throw new ParseException(start, end, "not implemented");
        }
    }

    /**
     * Weighted is an intermediate pattern collapsed by List, it should never be returned.
     */
    record Weighted(
            int start, int end,
            int weight,
            @Nullable PatternTree pattern
    ) implements PatternTree {
        @Override
        public @NotNull Pattern into() throws ParseException {
            throw new ParseException(start, end, "intermediate, this should not happen.");
        }
    }

    record Error(int start, int end) implements PatternTree {
        @Override
        public @NotNull Pattern into() throws ParseException {
            throw new ParseException(start, end, "not implemented");
        }
    }

    // Helpers

    record NamespaceId(
            int start, int end,
            int colon, // -1 if not present
            @NotNull String left,
            @Nullable String right
    ) {
    }

    record PropertyList(
            int start, int end,
            // Both -1 if not present
            int openBracket, int closeBracket,
            @NotNull List<Property> properties
    ) {

        public @UnknownNullability Property get(int index) {
            return properties.get(index);
        }

        public int size() {
            return properties.size();
        }

        public record Property(
                int start, int end,
                @NotNull String key,
                @Nullable String value
        ) implements Tree {

        }
    }


}
