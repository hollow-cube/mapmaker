package net.hollowcube.terraform.legacy.session;

import net.hollowcube.terraform.legacy.schem.Schematics;
import net.hollowcube.terraform.util.Transform;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A named clipboard slot. Holds the new engine's {@link net.hollowcube.terraform.clipboard.Clipboard}, saved
 * as a Sponge schematic (any pending transform applied).
 */
public class Clipboard {

    public static final NetworkBuffer.Type<Clipboard> NETWORK_TYPE = NetworkBufferTemplate.template(
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
}
