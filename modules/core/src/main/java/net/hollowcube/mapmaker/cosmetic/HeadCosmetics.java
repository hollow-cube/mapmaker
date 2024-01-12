package net.hollowcube.mapmaker.cosmetic;

import org.jetbrains.annotations.NotNull;

public final class HeadCosmetics {

    public static Cosmetic TOP_HAT = builder("top_hat").rarity(Cosmetic.Rarity.COMMON).build();


    public static @NotNull Cosmetic.Builder builder(@NotNull String id) {
        return new Cosmetic.Builder("head", id);
    }
}
