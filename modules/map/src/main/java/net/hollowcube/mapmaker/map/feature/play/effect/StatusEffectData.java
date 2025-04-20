package net.hollowcube.mapmaker.map.feature.play.effect;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.feature.play.setting.SavedMapSettings;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class StatusEffectData extends BaseEffectData {
    public static final StructCodec<StatusEffectData> CODEC = StructCodec.struct(
            // BaseEffectData
            "name", Codec.STRING.optional(""), StatusEffectData::name,
            "progressIndex", Codec.INT.optional(-1), StatusEffectData::progressIndex,
            "timeLimit", Codec.INT.optional(0), StatusEffectData::timeLimit,
            "resetHeight", Codec.INT.optional(NO_RESET_HEIGHT), StatusEffectData::resetHeight,
            "clearPotionEffects", Codec.BOOLEAN.optional(false), StatusEffectData::clearPotionEffects,
            StructCodec.INLINE, PotionEffectList.CODEC, StatusEffectData::potionEffects,
            "teleport", ExtraCodecs.POS.optional(), StatusEffectData::teleport,
            "items", HotbarItems.CODEC.optional(HotbarItems.EMPTY), StatusEffectData::items,
            // StatusEffectData
            "repeatable", Codec.BOOLEAN.optional(false), StatusEffectData::repeatable,
            "extraTime", Codec.INT.optional(0), StatusEffectData::extraTime,
            "settings", SavedMapSettings.CODEC.optional(), StatusEffectData::settings,
            StatusEffectData::new
    );

    private boolean repeatable;
    private int extraTime;

    public StatusEffectData(
            @NotNull String name, int progressIndex, int timeLimit,
            int resetHeight, boolean clearPotionEffects,
            @NotNull PotionEffectList potionEffects,
            @Nullable Pos teleport, @NotNull HotbarItems items,
            boolean repeatable, int extraTime,
            @Nullable SavedMapSettings settings
    ) {
        super(name, progressIndex, timeLimit, resetHeight, clearPotionEffects, potionEffects, teleport, items, settings);
        this.repeatable = repeatable;
        this.extraTime = extraTime;
    }

    public boolean repeatable() {
        return repeatable;
    }

    public int extraTime() {
        return extraTime;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public void setExtraTime(int extraTime) {
        this.extraTime = extraTime;
    }

    @Override
    public void sendDebugInfo(@NotNull Player player) {
        super.sendDebugInfo(player);
        player.sendMessage("Repeatable: " + repeatable);
        player.sendMessage("Extra Time: " + (extraTime == 0 ? "none" : extraTime));
    }
}
