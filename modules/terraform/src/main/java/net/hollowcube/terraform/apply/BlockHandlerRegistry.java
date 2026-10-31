package net.hollowcube.terraform.apply;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.Unmodifiable;

import java.util.BitSet;
import java.util.Map;

final class BlockHandlerRegistry {
    private static final BitSet BLOCK_ENTITY_STATES; // All cases where the block has a registry handler

    static {
        BitSet states = new BitSet();
        for (Block block : Block.values()) {
            if (block.blockEntityType() == null) continue;
            for (Block state : block.possibleStates()) states.set(state.stateId());
        }
        BLOCK_ENTITY_STATES = states;
    }

    private final @Unmodifiable Map<Key, BlockHandler> custom;
    // States whose cells need a chunk entry even when the buffer carries no block entity for them.
    private final BitSet entryStates;

    BlockHandlerRegistry() {
        this(Map.of());
    }

    BlockHandlerRegistry(Map<Key, ? extends BlockHandler> custom) {
        this.custom = Map.copyOf(custom);
        this.entryStates = (BitSet) BLOCK_ENTITY_STATES.clone();
        for (Key key : custom.keySet()) {
            Block block = Block.fromKey(key);
            if (block == null)
                throw new IllegalArgumentException("block handler for unknown block: " + key);
            for (Block state : block.possibleStates()) entryStates.set(state.stateId());
        }
    }

    Block attach(Block block) {
        if (block.handler() != null || !needsEntry(block.stateId())) {
            return block;
        }

        BlockHandler handler = custom.get(block.key());
        return handler == null ? block : block.withHandler(handler);
    }

    boolean needsEntry(int stateId) {
        return entryStates.get(stateId);
    }

}
