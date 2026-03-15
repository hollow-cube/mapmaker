package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.backpack.Rarity;
import net.hollowcube.mapmaker.cosmetic.impl.ModelCosmeticImpl;

public final class Hats {

    //    public static Cosmetic FLOWER_HAT = builder("flower_hat").rarity(Rarity.COMMON).build();
    public static Cosmetic TOP_HAT = builder("top_hat").rarity(Rarity.COMMON).build();
    public static Cosmetic SUNGLASSES = builder("sunglasses").rarity(Rarity.COMMON).build();
    public static Cosmetic HARD_HAT = builder("hard_hat").rarity(Rarity.COMMON).build();
    public static Cosmetic CROWN = builder("crown").rarity(Rarity.COMMON).build();
    public static Cosmetic STRAW_HAT = builder("straw_hat").rarity(Rarity.COMMON).build();
    //    public static Cosmetic PIRATE_HAT = builder("pirate_hat").rarity(Rarity.RARE).build();
    public static Cosmetic CAKE_HAT = builder("cake_hat")
            .hidden()
            .rarity(Rarity.RARE)
            .tags(CosmeticTag.LIMITED_TIME)
            .build();
    public static Cosmetic CLOWN_MASK = builder("clown_mask").rarity(Rarity.RARE).build();
    public static Cosmetic BIKERS_HELMET = builder("bikers_helmet").rarity(Rarity.RARE).build();
    public static Cosmetic SAMURAI_HELMET = builder("samurai_helmet").rarity(Rarity.RARE).build();
    public static Cosmetic KITSUNE_MASK = builder("kitsune_mask").rarity(Rarity.RARE).build();
    public static Cosmetic APPRENTICE_HAT = builder("apprentice_hat").rarity(Rarity.RARE).build();
    public static Cosmetic WIZARD_HAT = builder("wizard_hat").rarity(Rarity.RARE).build();
    //    public static Cosmetic MINERS_HELMET = builder("miners_helmet").rarity(Rarity.RARE).build();
    public static Cosmetic KNIGHT_HELMET = builder("knight_helmet").rarity(Rarity.RARE).build();
    public static Cosmetic EVIL_CLOWN_MASK = builder("evil_clown_mask").rarity(Rarity.EPIC).build();
    public static Cosmetic ONI_MASK = builder("oni_mask").rarity(Rarity.EPIC).build();
    public static Cosmetic SHARK_HAT = builder("shark_hat").rarity(Rarity.EPIC).build();

    // Christmas Event
    public static Cosmetic ELF_HAT = builder("elf_hat")
            .hidden()
            .rarity(Rarity.COMMON)
            .tags(CosmeticTag.LIMITED_TIME)
            .build();
    public static Cosmetic SNOWMAN_HEAD = builder("snowman_head")
            .hidden()
            .rarity(Rarity.RARE)
            .tags(CosmeticTag.LIMITED_TIME)
            .build();
    public static Cosmetic SANTA_HAT = builder("santa_hat")
            .hidden()
            .rarity(Rarity.EPIC)
            .tags(CosmeticTag.LIMITED_TIME)
            .build();

    public static Cosmetic.Builder builder(String id) {
        return new Cosmetic.Builder(CosmeticType.HAT, id)
                .impl(ModelCosmeticImpl::new);
    }

    private Hats() {
    }
}
