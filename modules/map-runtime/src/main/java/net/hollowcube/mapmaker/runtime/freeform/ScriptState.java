package net.hollowcube.mapmaker.runtime.freeform;

import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.Nullable;

public class ScriptState {

    public static Codec<ScriptState> CODEC = StructCodec.struct(
            "saveData", Codec.RAW_VALUE.optional(), ScriptState::saveData,
            ScriptState::new);
    // Todo should not be a play state of course
    public static final SaveStateType.Serializer<ScriptState> SERIALIZER = SaveStateType.serializer("playState", CODEC, HCDataTypes.PLAY_STATE);

    private @Nullable Codec.RawValue saveData;

    public ScriptState(@Nullable Codec.RawValue saveData) {
        this.saveData = saveData;
    }

    public @Nullable Codec.RawValue saveData() {
        return this.saveData;
    }

    public void saveData(@Nullable Codec.RawValue saveData) {
        this.saveData = saveData;
    }

}
