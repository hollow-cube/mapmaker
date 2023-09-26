package net.hollowcube.terraform.compat.axiom.world.property;

import org.jetbrains.annotations.NotNull;

public record Category(
        @NotNull String name,
        boolean localizeName
) {
}
