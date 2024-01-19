package net.hollowcube.mapmaker.cosmetic;

import org.jetbrains.annotations.NotNull;

public final class HeadCosmetics {

    public static Cosmetic CROWN = builder("crown").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic PIRATE_HAT = builder("pirate_hat").rarity(Cosmetic.Rarity.COMMON).build();


    public static @NotNull Cosmetic.Builder builder(@NotNull String id) {
        return new Cosmetic.Builder(CosmeticType.HEAD, id);
    }
}
