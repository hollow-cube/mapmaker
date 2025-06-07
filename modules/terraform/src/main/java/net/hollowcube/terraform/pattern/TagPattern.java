package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.task.edit.WorldView;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.registry.Registry;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

public record TagPattern(
        @NotNull List<Block> blocks,
        boolean randomState
) implements Pattern {
    private static final Registry<Block> BLOCK_REGISTRY = MinecraftServer.process().blocks();

    public TagPattern(@NotNull String tag, boolean randomState) {
        this(lookupTag(tag), randomState);
    }

    @Override
    public @NotNull Block blockAt(@NotNull WorldView world, @NotNull Point blockPosition) {
        var block = blocks.get(world.random().nextInt(blocks.size()));
        if (!randomState) return block;

        var states = block.possibleStates();
        return states.stream()
                .skip(world.random().nextInt(states.size()))
                .findFirst().orElse(Block.AIR);
    }

    private static @NotNull List<Block> lookupTag(@NotNull String nsid) {
        //todo this should lookup from Terraform registry
        var tag = BLOCK_REGISTRY.getTag(Key.key(nsid));
        Check.notNull(tag, "no such tag: " + nsid);

        return StreamSupport.stream(tag.spliterator(), false)
                .map(key -> Block.fromKey(key.key()))
                .filter(Objects::nonNull) // This is a sanity check
                .toList();
    }
}
