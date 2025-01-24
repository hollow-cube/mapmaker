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

public class CheckpointEffectData extends BaseEffectData {
    public static final int NO_LIVES = 0;

    public static final Codec<CheckpointEffectData> CODEC = RecordCodecBuilder.create(i -> i.group(
            // BaseEffectData
            Codec.STRING.optionalFieldOf("name", "").forGetter(CheckpointEffectData::name),
            Codec.INT.optionalFieldOf("progressIndex", -1).forGetter(CheckpointEffectData::progressIndex),
            Codec.INT.optionalFieldOf("timeLimit", 0).forGetter(CheckpointEffectData::timeLimit),
            Codec.INT.optionalFieldOf("resetHeight", NO_RESET_HEIGHT).forGetter(CheckpointEffectData::resetHeight),
            Codec.BOOL.optionalFieldOf("clearPotionEffects", false).forGetter(CheckpointEffectData::clearPotionEffects),
            PotionEffectList.NULL_MAPPED_CODEC.forGetter(CheckpointEffectData::potionEffects),
            ExtraCodecs.POS.optionalFieldOf("teleport").forGetter(CheckpointEffectData::teleport),
            HotbarItems.CODEC.optionalFieldOf("items", HotbarItems.EMPTY).forGetter(CheckpointEffectData::items),
            // CheckpointEffectData
            Codec.INT.optionalFieldOf("lives", NO_LIVES).forGetter(CheckpointEffectData::lives),
            SavedMapSettings.CODEC.fieldOf("settings").orElseGet(s -> {}, SavedMapSettings::new).forGetter(CheckpointEffectData::settings)
    ).apply(i, CheckpointEffectData::new));

    private int lives;

    public CheckpointEffectData(
            String name, int progressIndex, int timeLimit,
            int resetHeight, boolean clearPotionEffects,
            PotionEffectList potionEffects,
            Optional<Pos> teleport,
            HotbarItems items,
            int lives,
            SavedMapSettings settings
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
