package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;

public class EditLivesAction extends AbstractAction<EditLivesAction.Data> {
    private static final int DEFAULT_LIVES = 0; // Disables the lives mechanic.

    public enum Mode {
        SET, ADD, SUBTRACT;

        public static final Codec<Mode> CODEC = Codec.Enum(Mode.class);
    }

    public record Data(@NotNull Mode mode, int lives) {
        public static final StructCodec<Data> CODEC = StructCodec.struct(
                "mode", Mode.CODEC.optional(Mode.SET), Data::mode,
                "value", ExtraCodecs.clamppedInt(0, 10).optional(DEFAULT_LIVES), Data::lives,
                Data::new);
    }

    public EditLivesAction() {
        super("mapmaker:lives", Data.CODEC);
    }
}
