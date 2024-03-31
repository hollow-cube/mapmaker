package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.backpack.Rarity;
import org.jetbrains.annotations.NotNull;

public final class Particles {

    public static Cosmetic CLOUD = builder("cloud").rarity(Rarity.COMMON).build();
    public static Cosmetic BUBBLE = builder("bubble").rarity(Rarity.COMMON).build();
    public static Cosmetic NOTE = builder("note").rarity(Rarity.COMMON).build();

    public static @NotNull Cosmetic.Builder builder(@NotNull String id) {
        return new Cosmetic.Builder(CosmeticType.PARTICLE, id);
    }
}
