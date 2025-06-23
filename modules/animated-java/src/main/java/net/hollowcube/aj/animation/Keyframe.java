package net.hollowcube.aj.animation;

import net.minestom.server.coordinate.Vec;
import org.jetbrains.annotations.NotNull;

public sealed interface Keyframe {

    record ConstantKeyframe(int tick, @NotNull Vec value) implements Keyframe {

    }

    // todo
//    record MolangKeyframe(int tick) {
//
//    }

}
