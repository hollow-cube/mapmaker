package net.hollowcube.mapmaker.map.feature.play.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.feature.play.setting.SavedMapSettings;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class StatusEffectData extends BaseEffectData {
    public static final Codec<StatusEffectData> CODEC = RecordCodecBuilder.create(i -> i.group(
            // BaseEffectData
            Codec.STRING.lenientOptionalFieldOf("name", "").forGetter(StatusEffectData::name),
            Codec.INT.lenientOptionalFieldOf("progressIndex", -1).forGetter(StatusEffectData::progressIndex),
            Codec.INT.lenientOptionalFieldOf("timeLimit", 0).forGetter(StatusEffectData::timeLimit),
            Codec.INT.lenientOptionalFieldOf("resetHeight", NO_RESET_HEIGHT).forGetter(StatusEffectData::resetHeight),
            Codec.BOOL.lenientOptionalFieldOf("clearPotionEffects", false).forGetter(StatusEffectData::clearPotionEffects),
            PotionEffectList.NULL_MAPPED_CODEC.forGetter(StatusEffectData::potionEffects),
            ExtraCodecs.POS.lenientOptionalFieldOf("teleport").forGetter(StatusEffectData::teleport),
            HotbarItems.CODEC.lenientOptionalFieldOf("items", HotbarItems.EMPTY).forGetter(StatusEffectData::items),
            // StatusEffectData
            Codec.BOOL.lenientOptionalFieldOf("repeatable", false).forGetter(StatusEffectData::repeatable),
            Codec.INT.lenientOptionalFieldOf("extraTime", 0).forGetter(StatusEffectData::extraTime),
            SavedMapSettings.CODEC.fieldOf("settings").orElseGet(s -> {}, SavedMapSettings::new).forGetter(StatusEffectData::settings)
    ).apply(i, StatusEffectData::new));

    private boolean repeatable;
    private int extraTime;

    public StatusEffectData(
            String name, int progressIndex, int timeLimit,
            int resetHeight, boolean clearPotionEffects,
            PotionEffectList potionEffects,
            Optional<Pos> teleport,
            HotbarItems items,
            boolean repeatable,
            int extraTime,
            SavedMapSettings settings
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
