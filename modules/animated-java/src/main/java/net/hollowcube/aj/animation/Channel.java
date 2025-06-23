package net.hollowcube.aj.animation;

import net.minestom.server.codec.Codec;

public enum Channel {
    POSITION,
    ROTATION,
    SCALE;

    public static final Codec<Channel> CODEC = Codec.Enum(Channel.class);
}
