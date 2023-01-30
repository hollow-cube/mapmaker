package net.hollowcube.terraform.history;

import net.hollowcube.terraform.instance.Schematic;
import net.hollowcube.terraform.instance.SchematicReader;
import net.hollowcube.terraform.session.LocalSession;
import org.jetbrains.annotations.NotNull;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public interface Change {

    static @NotNull Change of(@NotNull Schematic undo, @NotNull Schematic redo) {
        return new SchematicChange(undo, redo);
    }

    static @NotNull Change fromNBT(@NotNull NBTCompound nbt) {
        try {
            return new SchematicChange(
                    SchematicReader.read(new ByteArrayInputStream(nbt.getByteArray("undo").copyArray())),
                    SchematicReader.read(new ByteArrayInputStream(nbt.getByteArray("redo").copyArray()))
            );
        } catch (IOException e) {
            // No exception
            throw new RuntimeException(e);
        }
    }

    void undo(@NotNull LocalSession session);

    void redo(@NotNull LocalSession session);

    @NotNull NBTCompound toNBT();

}
