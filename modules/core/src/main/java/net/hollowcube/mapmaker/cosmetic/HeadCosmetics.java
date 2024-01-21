package net.hollowcube.mapmaker.cosmetic;

import org.jetbrains.annotations.NotNull;

public final class HeadCosmetics {

    public static Cosmetic CAT_EARS_V1 = builder("cat_earsv1").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic CAT_EARS_V2 = builder("cat_earsv2").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic CLOWN_MASK = builder("clown_mask").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic CROWN = builder("crown").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic DOG_EARS_V1 = builder("dog_earsv1").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic DOG_EARS_V2 = builder("dog_earsv2").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic EVIL_CLOWN_MASK = builder("evil_clown_mask").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic HARD_HAT = builder("hard_hat").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic HAZARD_HELM_V2 = builder("hazard_helmv2").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic HAZARD_HELM_V3 = builder("hazard_helmv3").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic KNIGHT_V1 = builder("knightv1").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic KNIGHT_V2 = builder("knightv2").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic PIRATE_HAT = builder("pirate_hat").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic SAMURAI_HELM_MASKED = builder("samurai_helm_mask").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic SAMURAI_HELM_UNMASKED = builder("samurai_helm_unmasked").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic SUNGLASSES_V1 = builder("sunglassesv1").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic SUNGLASSES_V2 = builder("sunglassesv2").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic TOP_HAT = builder("top_hat").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic WIZARD_CAP = builder("wizard_cap").rarity(Cosmetic.Rarity.COMMON).build();
    public static Cosmetic WIZARD_CAP_V2 = builder("wizard_cap_v2").rarity(Cosmetic.Rarity.COMMON).build();


    public static @NotNull Cosmetic.Builder builder(@NotNull String id) {
        return new Cosmetic.Builder(CosmeticType.HEAD, id);
    }
}
