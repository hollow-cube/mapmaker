package net.hollowcube.terraform.clipboard;

import net.hollowcube.util.schem.Rotation;
import net.hollowcube.util.schem.Schematic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Clipboard {
    private Schematic current = null;
    private Rotation currentRotation = Rotation.NONE;

    public void set(@NotNull Schematic schematic) {
        this.current = schematic;
    }

    public @Nullable Schematic get() {
        return current;
    }

    public Rotation getCurrentRotation() {
        return currentRotation;
    }

    public void setCurrentRotation(Rotation currentRotation) {
        this.currentRotation = currentRotation;
    }
}
