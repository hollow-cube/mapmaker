package net.hollowcube.compat.axiom.data.buffers;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdDictDecompress;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.utils.validate.Check;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;

public record AxiomBlockEntityData(
        int size, byte dict, byte[] data
) {

    private static final ZstdDictDecompress DECOMPRESSOR;
    static {
        try (var stream = AxiomBlockEntityData.class.getResourceAsStream("/axiom/block_entities_v1.dict")) {
            var bytes = Objects.requireNonNull(stream, "Failed to load axiom BE dictionary").readAllBytes();
            DECOMPRESSOR = new ZstdDictDecompress(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load block entity dictionary", e);
        }
    }

    public CompoundBinaryTag decompress() throws IOException {
        Check.stateCondition(this.dict != 0, "Unsupported dictionary version: " + this.dict);
        byte[] decompressed = Zstd.decompress(this.data, DECOMPRESSOR, this.size);
        return BinaryTagIO.reader().read(new ByteArrayInputStream(decompressed));
    }
}