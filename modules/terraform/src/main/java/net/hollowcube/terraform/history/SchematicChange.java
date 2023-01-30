package net.hollowcube.terraform.history;

import net.hollowcube.terraform.instance.Schematic;
import net.hollowcube.terraform.instance.SchematicWriter;
import net.hollowcube.terraform.session.LocalSession;
import net.hollowcube.util.schem.Rotation;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTByteArray;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

public record SchematicChange(
        @NotNull Schematic undo,
        @NotNull Schematic redo
) implements Change {
    @Override
    public void undo(@NotNull LocalSession session) {
        undo.build(Rotation.NONE, null).apply(session.instance(), null);
    }

    @Override
    public void redo(@NotNull LocalSession session) {
        redo.build(Rotation.NONE, null).apply(session.instance(), null);
    }

    @Override
    public @NotNull NBTCompound toNBT() {
        var root = new MutableNBTCompound();
        root.set("undo", new NBTByteArray(SchematicWriter.write(undo)));
        root.set("redo", new NBTByteArray(SchematicWriter.write(redo)));
        return root.toCompound();
    }
}
