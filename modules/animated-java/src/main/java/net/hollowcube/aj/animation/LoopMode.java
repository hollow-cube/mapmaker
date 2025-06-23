package net.hollowcube.aj.animation;

import net.minestom.server.codec.Codec;

public enum LoopMode {
    LOOP,
    ONCE,
    HOLD,
    ; // todo other values

    public static final Codec<LoopMode> CODEC = Codec.Enum(LoopMode.class);
}
