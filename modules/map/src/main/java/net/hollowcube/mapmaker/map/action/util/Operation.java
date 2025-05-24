package net.hollowcube.mapmaker.map.action.util;

import net.minestom.server.codec.Codec;

public enum Operation {
    SET, ADD, SUBTRACT;

    public static final Codec<Operation> CODEC = Codec.Enum(Operation.class);
}
