package net.hollowcube.mapmaker.map.setting;

import net.minestom.server.codec.Codec;
import net.minestom.server.utils.Either;

@SuppressWarnings("UnstableApiUsage")
public enum NoSpectateMode {
    OFF, // Spectating is always enabled
    ON, // Spectating is always disabled
    AFTER_COMPLETION, // Spectating is only available if the player has 1 completed save state.
    ;

    public static final Codec<NoSpectateMode> CODEC = Codec
            .Either(Codec.Enum(NoSpectateMode.class), Codec.BOOLEAN)
            .transform(
                    either -> switch (either) {
                        case Either.Left(var value) -> value;
                        case Either.Right(var value) -> value ? ON : OFF;
                    },
                    mode -> switch (mode) {
                        case OFF -> Either.right(false);
                        case ON -> Either.right(true);
                        case AFTER_COMPLETION -> Either.left(mode);
                    }
            );
}
