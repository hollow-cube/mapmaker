package net.hollowcube.mapmaker;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class PackContext {

    private Path resources;
    private Path out;

    public PackContext(Path resources, Path out) {
        this.resources = resources;
        this.out = out;
    }

    public @NotNull Path resources() {
        return resources;
    }

    public @NotNull Path out() {
        return out;
    }
}
