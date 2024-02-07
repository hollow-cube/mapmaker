package net.hollowcube.terraform.pattern;

import net.hollowcube.terraform.task.edit.WorldView;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public record TypeStatePattern(
        int blockId, // -1 for keep existing
        @NotNull Map<String, String> states
) implements Pattern {

    public TypeStatePattern {
        if (blockId != -1) {
            Check.notNull(Block.fromBlockId(blockId), "Unknown block id: " + blockId);
        }
    }

    @Override
    public @NotNull Block blockAt(@NotNull WorldView world, @NotNull Point blockPosition) {
        var existing = world.getBlock(blockPosition);
        var block = existing;

        // Replace the block if necessary
        if (blockId != -1 && existing.id() != blockId) {
            block = Block.fromBlockId(blockId);
        }

        // Apply the known states to the block
        //noinspection DataFlowIssue (checked above)
        var validProperties = block.properties().keySet();
        var newProperties = new HashMap<String, String>();
        for (var entry : states.entrySet()) {
            if (validProperties.contains(entry.getKey())) {
                newProperties.put(entry.getKey(), entry.getValue());
            }
        }

        // Apply any other properties from the original block
        for (var entry : existing.properties().entrySet()) {
            if (!states.containsKey(entry.getKey()) && validProperties.contains(entry.getKey())) {
                newProperties.put(entry.getKey(), entry.getValue());
            }
        }

        if (newProperties.isEmpty()) {
            return block;
        } else {
            return block.withProperties(newProperties);
        }
    }

}
