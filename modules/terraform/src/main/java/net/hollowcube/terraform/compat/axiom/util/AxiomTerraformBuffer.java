package net.hollowcube.terraform.compat.axiom.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.hollowcube.common.util.Either;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockBuffer;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockEntityData;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.util.PaletteUtil;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public record AxiomTerraformBuffer(
        Long2ObjectMap<AxiomPalette> sections
) implements BlockBuffer {

    public AxiomTerraformBuffer(AxiomBlockBuffer buffer) {
        this(OpUtils.build(new Long2ObjectArrayMap<>(), map -> Long2ObjectMaps.fastForEach(buffer.updates(), entry -> {
            long key = entry.getLongKey();
            map.put(key, new AxiomPalette(entry.getValue(), buffer.blockEntities().get(key)));
        })));
    }

    @Override
    public void forEachSection(@NotNull SectionConsumer consumer) {
        Long2ObjectMaps.fastForEach(this.sections, entry -> {
            long key = entry.getLongKey();
            int x = PaletteUtil.unpackX(key);
            int y = PaletteUtil.unpackY(key);
            int z = PaletteUtil.unpackZ(key);
            consumer.accept(x, y, z, entry.getValue());
        });
    }

    @Override
    public long sizeBytes() {
        return this.sections.values().stream().mapToLong(AxiomPalette::sizeBytes).sum();
    }

    private record AxiomPalette(
            Either<Block, Block[]> palette,
            Short2ObjectMap<AxiomBlockEntityData> blockEntities
    ) implements net.hollowcube.terraform.buffer.palette.Palette {

        @Override
        public @Nullable Block get(int x, int y, int z) {
            return this.palette.map(
                    Function.identity(),
                    blocks -> blocks[x + 16 * (z + 16 * y)]
            );
        }

        @Override
        public long sizeBytes() {
            return (long) palette.map(
                    $ -> Integer.BYTES,
                    blocks -> blocks.length * Integer.BYTES
            );
        }
    }
}
