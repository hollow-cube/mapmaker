package net.hollowcube.terraform.session;

import net.hollowcube.terraform.math.AffineTransform;
import net.hollowcube.terraform.schem.Rotation;
import net.hollowcube.terraform.schem.Schematic;
import net.hollowcube.terraform.schem.SchematicBuilder;
import net.minestom.server.coordinate.Point;
import net.minestom.server.utils.validate.Check;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Clipboard {
    public static final @NotNull String DEFAULT = "default";
    public static final @NotNull
    @RegExp String NAME_REGEX = "[a-z0-9_]+";

    private final String name;

    private Schematic schematic; // The current block data stored in this clipboard.
    private List<AffineTransform> transforms; // The transforms, applied in list order.

    public Clipboard(@NotNull String name) {
        this.name = name;

        this.schematic = null;
        this.transforms = new ArrayList<>();
    }

    public @NotNull String name() {
        return name;
    }

    public boolean isEmpty() {
        return schematic == null;
    }

    public void setData(@Nullable Schematic schematic) {
        this.schematic = schematic;
        this.transforms = new ArrayList<>();
    }

    public @NotNull CompletableFuture<Void> apply(@NotNull LocalSession session, @NotNull Point pos) {
        Check.stateCondition(isEmpty(), "Clipboard is empty");
        //todo rewrite to use actions and add to history stack

        var newSchem = new SchematicBuilder();
        schematic.apply(Rotation.NONE, (p, block) -> {
            for (var transform : transforms) {
                p = transform.apply2(p);
            }
            newSchem.addBlock(p, block);
        });

        var future = new CompletableFuture<Void>();
        newSchem.build().build(Rotation.NONE, null)
                .apply(session.instance(), pos, () -> future.complete(null));
        return future;
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

}
