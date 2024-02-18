package net.hollowcube.mapmaker.map.feature.play.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.util.dfu.ExtraCodecs;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class StatusEffectData extends BaseEffectData {
    public static final Codec<StatusEffectData> CODEC = RecordCodecBuilder.create(i -> i.group(
            // BaseEffectData
            Codec.STRING.optionalFieldOf("name", "").forGetter(StatusEffectData::name),
            Codec.INT.optionalFieldOf("progressIndex", -1).forGetter(StatusEffectData::progressIndex),
            Codec.INT.optionalFieldOf("timeLimit", 0).forGetter(StatusEffectData::timeLimit),
            Codec.INT.optionalFieldOf("resetHeight", NO_RESET_HEIGHT).forGetter(StatusEffectData::resetHeight),
            Codec.BOOL.optionalFieldOf("clearPotionEffects", false).forGetter(StatusEffectData::clearPotionEffects),
            PotionEffectList.NULL_MAPPED_CODEC.forGetter(StatusEffectData::potionEffects),
            ExtraCodecs.POS.optionalFieldOf("teleport").forGetter(StatusEffectData::teleport),
            // StatusEffectData
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(StatusEffectData::repeatable),
            Codec.INT.optionalFieldOf("extraTime", 0).forGetter(StatusEffectData::extraTime)
    ).apply(i, StatusEffectData::new));

    private boolean repeatable;
    private int extraTime;

    public StatusEffectData(
            String name, int progressIndex, int timeLimit,
            int resetHeight, boolean clearPotionEffects,
            PotionEffectList potionEffects,
            Optional<Pos> teleport, boolean repeatable,
            int extraTime
    ) {
        super(name, progressIndex, timeLimit, resetHeight, clearPotionEffects, potionEffects, teleport);
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
