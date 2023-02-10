package net.hollowcube.terraform.mask.script;

import net.hollowcube.terraform.mask.BlockMask;
import net.hollowcube.terraform.mask.Mask;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.List;
import java.util.Map;

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
                throw new MaskParseException(end, end, "Expected closing bracket ']'");
            }

            // Attempt to parse the block properties.
            var properties = Map.<String, String>of();

            return new BlockMask(block.id(), properties);
        }

        record Property(
                int start, int end,
                String key,
                @Nullable Tree value
        ) implements Tree {

        }
    }

    record Number(int start, int end, double value) implements Tree {

    }

    record Placeholder() {

    }


    // Combinators

    record Prefix(int start, int end, Type type) {
        enum Type {
            NOT
        }
    }

    record Binary() {
    }

    record Named() {
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
