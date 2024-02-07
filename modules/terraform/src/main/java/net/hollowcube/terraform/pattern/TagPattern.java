package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.gamedata.tags.Tag;
import net.minestom.server.gamedata.tags.TagManager;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public record TagPattern(
        @NotNull List<Block> blocks,
        boolean randomState
) implements Pattern {
    private static final TagManager TAG_MANAGER = MinecraftServer.getTagManager();

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
        var tag = TAG_MANAGER.getTag(Tag.BasicType.BLOCKS, nsid);
        Check.notNull(tag, "no such tag: " + nsid);

        return tag.getValues().stream()
                .map(Block::fromNamespaceId)
                .filter(Objects::nonNull) // This is a sanity check
                .toList();
    }
}
