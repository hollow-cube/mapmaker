package net.hollowcube.terraform.legacy.session;

import com.github.luben.zstd.Zstd;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.legacy.schem.SchemUpgrader;
import net.hollowcube.terraform.legacy.schem.Schematics;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.util.Transform;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A named clipboard slot. Holds the new engine's {@link net.hollowcube.terraform.clipboard.Clipboard}.
 */
public class Clipboard {

    /**
     * Session state version 2 on: the clipboard's buffer, origin, pending transform and metadata, zstd compressed.
     */
    public static final NetworkBuffer.Type<Clipboard> NETWORK_TYPE = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, Clipboard::name,
            new DataType().optional(), Clipboard::data,
            Clipboard::new
    );

    /**
     * Session state version 1: the clipboard as Sponge schematic bytes, any pending transform applied.
     */
    public static final NetworkBuffer.Type<Clipboard> NETWORK_TYPE_V1 = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, Clipboard::name,
            NetworkBuffer.BYTE_ARRAY.transform(
                    bytes -> {
                        try {
                            return new net.hollowcube.terraform.clipboard.Clipboard(Schematics.read(bytes));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    },
                    data -> Schematics.write(data.toSchematic())
            ).optional(), Clipboard::data,
            Clipboard::new
    );

    public static final @NotNull String DEFAULT = "default";
    @RegExp
    public static final @NotNull String NAME_REGEX = "[a-z0-9_]+";

    private final String name;

    private volatile net.hollowcube.terraform.clipboard.@Nullable Clipboard data;

    public Clipboard(@NotNull String name) {
        this(name, null);
    }

    private Clipboard(@NotNull String name, net.hollowcube.terraform.clipboard.@Nullable Clipboard data) {
        this.name = name;
        this.data = data;
    }

    public @NotNull String name() {
        return name;
    }

    public net.hollowcube.terraform.clipboard.@Nullable Clipboard data() {
        return data;
    }

    public boolean isEmpty() {
        return data == null;
    }

    public void setData(net.hollowcube.terraform.clipboard.@Nullable Clipboard data) {
        this.data = data;
    }

    public void clear() {
        this.data = null;
    }

    /**
     * Applies {@code transform} after any pending one. Does nothing to an empty clipboard.
     */
    public void applyTransform(@NotNull Transform transform) {
        var current = data;
        if (current != null) data = current.withTransform(transform);
    }

    private static final class DataType implements NetworkBuffer.Type<net.hollowcube.terraform.clipboard.Clipboard> {
        @Override
        public void write(@NotNull NetworkBuffer buffer, net.hollowcube.terraform.clipboard.@NotNull Clipboard value) {
            byte[] raw = NetworkBuffer.makeArray(out -> {
                out.write(NetworkBuffer.BLOCK_POSITION, value.origin());
                Transform t = value.transform();
                for (double entry : new double[]{
                        t.m00(), t.m01(), t.m02(), t.m03(),
                        t.m10(), t.m11(), t.m12(), t.m13(),
                        t.m20(), t.m21(), t.m22(), t.m23()
                }) out.write(NetworkBuffer.DOUBLE, entry);
                out.write(NetworkBuffer.NBT_COMPOUND, value.schematic().metadata());
                out.write(bufferType(), value.buffer());
            });
            buffer.write(NetworkBuffer.VAR_INT, raw.length);
            buffer.write(NetworkBuffer.BYTE_ARRAY, Zstd.compress(raw));
        }

        @Override
        public net.hollowcube.terraform.clipboard.@NotNull Clipboard read(@NotNull NetworkBuffer buffer) {
            int length = buffer.read(NetworkBuffer.VAR_INT);
            byte[] raw = Zstd.decompress(buffer.read(NetworkBuffer.BYTE_ARRAY), length);
            var in = NetworkBuffer.wrap(raw, 0, raw.length);

            var origin = in.read(NetworkBuffer.BLOCK_POSITION).asBlockVec();
            double[] m = new double[12];
            for (int i = 0; i < m.length; i++) m[i] = in.read(NetworkBuffer.DOUBLE);
            var transform = new Transform(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11]);
            var metadata = in.read(NetworkBuffer.NBT_COMPOUND);
            var schematic = new Schematic(in.read(bufferType()), origin, metadata);
            return new net.hollowcube.terraform.clipboard.Clipboard(schematic, transform);
        }

        private static NetworkBuffer.Type<EditBuffer> bufferType() {
            return EditBuffer.serializer(SchemUpgrader.INSTANCE, MinecraftServer.getBiomeRegistry());
        }
    }
}
