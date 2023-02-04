package net.hollowcube.terraform.mask;

import org.jetbrains.annotations.NotNull;

public class MockMask {

    public static @NotNull Mask none() {
        return (world, point, block) -> {throw new AssertionError("Mask should not be accessed");};
    }
}
