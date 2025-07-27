package net.hollowcube.mapmaker.runtime.parkour.action.util;

import net.minestom.server.codec.Codec;

public enum Operation {
    SET, ADD, SUBTRACT;

    public static final Codec<Operation> CODEC = Codec.Enum(Operation.class);

    public double apply(double current, double value) {
        return switch (this) {
            case SET -> value;
            case ADD -> current + value;
            case SUBTRACT -> current - value;
        };
    }

}
