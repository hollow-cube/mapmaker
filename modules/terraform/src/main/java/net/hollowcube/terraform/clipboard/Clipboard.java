package net.hollowcube.terraform.clipboard;

import net.hollowcube.util.schem.Schematic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Clipboard {
    private Schematic current = null;

    public void set(@NotNull Schematic schematic) {
        this.current = schematic;
    }

    public @Nullable Schematic get() {
        return current;
    }
}
