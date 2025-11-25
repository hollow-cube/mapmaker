package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.backpack.Rarity;
import net.hollowcube.mapmaker.cosmetic.impl.victory.FireworkVictoryEffectImpl;
import net.hollowcube.mapmaker.cosmetic.impl.victory.ParticleVictoryEffects;
import org.jetbrains.annotations.NotNull;

public final class VictoryEffects {

    public static Cosmetic EXPLOSION = builder("explosion").rarity(Rarity.COMMON).build();
    public static Cosmetic LIGHTNING = builder("lightning").rarity(Rarity.COMMON).build();
    public static Cosmetic FIREWORK = builder("firework").rarity(Rarity.COMMON).impl(FireworkVictoryEffectImpl::new).build();
    public static Cosmetic OMEGA = builder("omega").rarity(Rarity.RARE).build();
//    public static Cosmetic TRAIN = builder("train").rarity(Rarity.EPIC).build();

    // Christmas Event
    public static Cosmetic JOLLY = builder("jolly")
            .hidden()
            .rarity(Rarity.LEGENDARY)
            .tags(CosmeticTag.LIMITED_TIME)
            .impl(ParticleVictoryEffects.ChristmasExplosion::new)
            .build();

    public static @NotNull Cosmetic.Builder builder(@NotNull String id) {
        return new Cosmetic.Builder(CosmeticType.VICTORY_EFFECT, id);
    }
}
