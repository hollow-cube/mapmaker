package net.hollowcube.terraform.mask.script;

import net.hollowcube.terraform.mask.BlockMask;
import net.hollowcube.terraform.mask.Mask;
import net.hollowcube.terraform.mask.RandomNoiseMask;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.List;

public interface Tree {

    int start();

    int end();

    default @NotNull Mask toMask() throws MaskParseException {
        //todo more descriptive error but basically this cannot turn into a mask
        throw new MaskParseException("Not implemented");
    }

    // Primitives

    record BlockState(
            int start, int end,
            // Both -1 if not present
            int openBracket, int closeBracket,
            // Block name, eg `sto`
            @NotNull String block,
            // Block properties, eg `foo=bar`
            // Null if openBracket == -1, otherwise never null.
            @UnknownNullability List<Property> props
    ) implements Tree {

        @Override
        public @NotNull Mask toMask() throws MaskParseException {
            var block = Block.fromNamespaceId(NamespaceID.from(block()));
            if (block == null) {
                throw new MaskParseException(start, start + block().length(),
                        String.format("No such block: %s", block()));
            }

            // If there is no supplied block state, then we are good to just return the block.
            if (openBracket() == -1) {
                return new BlockMask(block.id());
            } else if (closeBracket() == -1) {
                // If open is provided then close also must be to be a valid mask
                throw new MaskParseException(end, end, "Expected property or ']'");
            }

            // Attempt to parse the block properties.
            var properties = new HashMap<String, String>();
            for (var prop : props()) {
                if (prop.value() == null) {
                    throw new MaskParseException(prop.end(), prop.end(), "Expected value");
                }

                try {
                    // Just calling to check if the property is valid.
                    //noinspection ResultOfMethodCallIgnored
                    block.withProperty(prop.key(), prop.value());
                } catch (IllegalArgumentException e) {
                    throw new MaskParseException(prop.start(), prop.end(),
                            String.format("%s has no property %s=%s", block.name(), prop.key(), prop.value()));
                }

                properties.put(prop.key(), prop.value());
            }

            return new BlockMask(block.id(), properties);
        }

        record Property(
                int start, int end,
                String key,
                @Nullable String value
        ) implements Tree {

        }
    }

    record Number(int start, int end, double value) implements Tree {

    }


    // Combinators

    record Prefix(
            int start, int end,
            @NotNull Type type,
            @Nullable Tree child
    ) implements Tree {
        public enum Type {
            NOT,
            UNIFORM_NOISE,
        }

        @Override
        public @NotNull Mask toMask() throws MaskParseException {
            if (child() == null)
                throw new MaskParseException(end, end, "Expected " + (type() == Type.UNIFORM_NOISE ? "number" : "mask"));
            return switch (type()) {
                case NOT -> Mask.not(child().toMask());
                case UNIFORM_NOISE -> new RandomNoiseMask(((Number) child()).value());
            };
        }
    }

    record Infix(
            int start, int end,
            @NotNull Type type,
            @NotNull Tree lhs,
            @Nullable Tree rhs
    ) implements Tree {
        public enum Type {
            OR, AND
        }

        @Override
        public @NotNull Mask toMask() throws MaskParseException {
            if (rhs() == null)
                throw new MaskParseException(end, end, "Expected mask");
            return switch (type()) {
                case OR -> Mask.or(lhs.toMask(), rhs().toMask());
                case AND -> Mask.and(lhs.toMask(), rhs().toMask());
            };
        }
    }


    // Types: mask, number, none (if at end of a mask)
    // Either an argument can be a mask or a number\
    // But also sometimes a placeholder should only suggest AND/OR
    // Another case is at the end of a block state, for example
    // `stone` should suggest `[` for args, `|`, `/`


    // ---Combinators---
    // Prefix (not, overlay, underlay)
    // Binary (or, and)
    // Named (#voronoi[args])
    //   How to know expected type of args?
}
