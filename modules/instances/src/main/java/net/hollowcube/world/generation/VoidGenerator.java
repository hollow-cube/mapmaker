package net.hollowcube.world.generation;

import net.minestom.server.instance.generator.GenerationUnit;
import net.minestom.server.instance.generator.Generator;
import org.jetbrains.annotations.NotNull;

public class VoidGenerator implements Generator {
    @Override
    public void generate(@NotNull GenerationUnit unit) {
        // Minestom worlds are void by default, so don't do anything
    }
}
