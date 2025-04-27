package net.hollowcube.mapmaker.map.feature.play.effect;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.feature.play.setting.SavedMapSettings;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class CheckpointEffectData extends BaseEffectData {
    public static final int NO_LIVES = 0;

    public static final StructCodec<CheckpointEffectData> CODEC = StructCodec.struct(
            // BaseEffectData
            "name", Codec.STRING.optional(""), CheckpointEffectData::name,
            "progressIndex", Codec.INT.optional(-1), CheckpointEffectData::progressIndex,
            "timeLimit", Codec.INT.optional(0), CheckpointEffectData::timeLimit,
            "resetHeight", Codec.INT.optional(NO_RESET_HEIGHT), CheckpointEffectData::resetHeight,
            "clearPotionEffects", Codec.BOOLEAN.optional(false), CheckpointEffectData::clearPotionEffects,
            StructCodec.INLINE, PotionEffectList.CODEC, CheckpointEffectData::potionEffects,
            "teleport", ExtraCodecs.POS.optional(), CheckpointEffectData::teleport,
            "items", HotbarItems.CODEC.optional(HotbarItems.EMPTY), CheckpointEffectData::items,
            // CheckpointEffectData
            "lives", Codec.INT.optional(NO_LIVES), CheckpointEffectData::lives,
            "settings", SavedMapSettings.CODEC.optional(), CheckpointEffectData::settings,
            CheckpointEffectData::new);

    public static CheckpointEffectData empty() {
        return CODEC.decode(Transcoder.NBT, CompoundBinaryTag.empty()).orElseThrow();
    }

    private int lives;

    public CheckpointEffectData(
            @NotNull String name, int progressIndex, int timeLimit,
            int resetHeight, boolean clearPotionEffects,
            @NotNull PotionEffectList potionEffects,
            @Nullable Pos teleport,
            @NotNull HotbarItems items,
            int lives,
            @Nullable SavedMapSettings settings
    ) {
        super(name, progressIndex, timeLimit, resetHeight, clearPotionEffects, potionEffects, teleport, items, settings);
        this.lives = lives;
    }

    public int lives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    @Override
    public void sendDebugInfo(@NotNull Player player) {
        super.sendDebugInfo(player);
        player.sendMessage("Lives: " + (lives < 0 ? "unlimited" : lives));
    }
}
