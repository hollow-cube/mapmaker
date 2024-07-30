package net.hollowcube.terraform.session;

import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.builder.SchematicBuilder;
import net.hollowcube.schem.reader.SpongeSchematicReader;
import net.hollowcube.schem.writer.SpongeSchematicWriter;
import net.hollowcube.terraform.util.math.AffineTransform;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minestom.server.network.NetworkBuffer.*;

@SuppressWarnings("UnstableApiUsage")
public class Clipboard {
    public static final @NotNull String DEFAULT = "default";
    @RegExp
    public static final @NotNull String NAME_REGEX = "[a-z0-9_]+";

    private final String name;

    private Schematic schematic = null; // The current block data stored in this clipboard.
    private List<AffineTransform> transforms; // The transforms, applied in list order.

    public Clipboard(@NotNull String name) {
        this.name = name;

        this.schematic = null;
        this.transforms = new ArrayList<>();
    }

    public Clipboard(@NotNull NetworkBuffer buffer) {
        this.name = buffer.read(STRING);

        this.schematic = buffer.readOptional(b -> new SpongeSchematicReader().read(b.read(BYTE_ARRAY)));
        this.transforms = new ArrayList<>();
    }

    public @NotNull String name() {
        return name;
    }

    public boolean isEmpty() {
        return schematic == null;
    }

    public void setData(@Nullable Schematic schematic) {
        clear();
        this.schematic = schematic;
    }

    public void clear() {
        this.schematic = null;
        this.transforms = new ArrayList<>();
    }

    @Deprecated
    public Schematic getSchematic() {
        return schematic;
    }

    @Deprecated
    public Schematic getSchematicWithRotations() {
//        var newSize = schematic.size();
//        for (var transform : transforms) {
//            newSize = transform.apply2(newSize);
//        }

//        var newSchem = SchematicBuilder.builder();
//        newSchem.offset(schematic.offset());
//        schematic.forEachBlock((p, block) -> {
//            for (var transform : transforms) {
//                p = transform.apply2(p);
//                if (hasRotationProperty(block)) {
//                    block = transform.applyToBlock(block);
//                }
//            }
//            newSchem.block(p, block);
//        });
//        return newSchem.build();
        return schematic;
    }

    public @NotNull CompletableFuture<Void> apply(@NotNull LocalSession session, @NotNull Point pos) {
        Check.stateCondition(isEmpty(), "Clipboard is empty");
        //todo rewrite to use actions and add to history stack
//
//        var newSize = schematic.size();
//        for (var transform : transforms) {
//            newSize = transform.apply2(newSize);
//        }

        var newSchem = SchematicBuilder.builder();
        schematic.forEachBlock((p, block) -> {
            for (var transform : transforms) {
                p = transform.apply2(p);
                if (hasRotationProperty(block)) {
                    block = transform.applyToBlock(block);
                }
            }
            newSchem.block(p, block);
        });

        var future = new CompletableFuture<Void>();
        newSchem.build().createBatch().apply(session.instance(), pos, () -> future.complete(null));
        return future;
    }

    private boolean hasRotationProperty(@NotNull Block block) {
        var properties = block.properties();
        return properties.containsKey("facing") || properties.containsKey("rotation") || properties.containsKey("axis");
    }

    public void rotate(double x, double y, double z) {
        var transform = new AffineTransform();
        //todo that transform class seems kinda borked, or i am misunderstanding something but realistically i should not have to do this (yzx or -)
        if (x != 0) transform = transform.rotateY(-x);
        if (y != 0) transform = transform.rotateX(-y);
        if (z != 0) transform = transform.rotateZ(-z);
        transforms.add(transform);
    }

    public void flip(boolean x, boolean y, boolean z) {
        var transform = new AffineTransform().scale(x ? -1 : 1, y ? -1 : 1, z ? -1 : 1);
        if (Boolean.getBoolean("terraform.feature.flip-inplace")) {
            //todo proper feature flags
            transform = transform.translate(schematic.offset().mul(x ? -1 : 0, y ? -1 : 0, z ? -1 : 0));
        }
        transforms.add(transform);
    }

    // Serialization

    @ApiStatus.Internal
    public void write(@NotNull NetworkBuffer buffer) {
        buffer.write(STRING, name);

        //todo writeoptional?? need network buffer type for schematic
        buffer.write(BOOLEAN, schematic != null);
        if (schematic != null) {
            buffer.write(BYTE_ARRAY, new SpongeSchematicWriter().write(schematic));
        }
    }
}
