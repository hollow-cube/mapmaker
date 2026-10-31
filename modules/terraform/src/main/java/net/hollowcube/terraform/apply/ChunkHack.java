package net.hollowcube.terraform.apply;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/// Internal theft of [DynamicChunk] and [LightingChunk] state.
///
/// Needs `--add-opens net.minestom.server/net.minestom.server.instance` under JPMS.
final class ChunkHack {
    private static final VarHandle ENTRIES;
    private static final VarHandle TICKABLE_MAP;
    private static final VarHandle OCCLUSION_DATA;

    static {
        try {
            var dynamic = MethodHandles.privateLookupIn(DynamicChunk.class, MethodHandles.lookup());
            ENTRIES = dynamic.findVarHandle(
                DynamicChunk.class,
                "entries",
                Int2ObjectOpenHashMap.class
            );
            TICKABLE_MAP = dynamic.findVarHandle(
                DynamicChunk.class,
                "tickableMap",
                Int2ObjectOpenHashMap.class
            );
            OCCLUSION_DATA = MethodHandles
                .privateLookupIn(LightingChunk.class, MethodHandles.lookup())
                .unreflectVarHandle(LightingChunk.class.getDeclaredField("occlusionData"));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @SuppressWarnings("unchecked")
    static Int2ObjectMap<Block> entries(Chunk chunk) {
        return (Int2ObjectMap<Block>) ENTRIES.get(chunk);
    }

    @SuppressWarnings("unchecked")
    static Int2ObjectMap<Block> tickableMap(Chunk chunk) {
        return (Int2ObjectMap<Block>) TICKABLE_MAP.get(chunk);
    }

    static void clearOcclusion(Chunk chunk) {
        if (chunk instanceof LightingChunk) OCCLUSION_DATA.setVolatile(chunk, (Object) null);
    }

    private ChunkHack() {}
}
