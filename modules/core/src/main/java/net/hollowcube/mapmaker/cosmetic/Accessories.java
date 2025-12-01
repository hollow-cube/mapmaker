package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.mapmaker.backpack.Rarity;
import net.hollowcube.mapmaker.cosmetic.impl.ModelCosmeticImpl;
import net.hollowcube.mapmaker.cosmetic.impl.accessory.ShrinkingDeviceImpl;
import org.jetbrains.annotations.NotNull;

public final class Accessories {

    public static Cosmetic DONUT = builder("donut").rarity(Rarity.COMMON).build();
    public static Cosmetic WRENCH = builder("wrench").rarity(Rarity.COMMON).build();
    public static Cosmetic TRAINING_SWORD = builder("training_sword").rarity(Rarity.COMMON).build();
    public static Cosmetic BURGER = builder("burger").rarity(Rarity.RARE).build();
    public static Cosmetic DYNAMITE = builder("dynamite").rarity(Rarity.RARE).build();
    public static Cosmetic KNIGHTS_SWORD = builder("knights_sword").rarity(Rarity.RARE).build();
    public static Cosmetic CYBERFIST = builder("cyberfist").rarity(Rarity.EPIC).build();
    public static Cosmetic COFFEE_CUP = builder("coffee_cup").rarity(Rarity.EPIC).build();
    public static Cosmetic DRILL = builder("drill").rarity(Rarity.LEGENDARY).build();
    public static Cosmetic EXCALIBUR = builder("excalibur").rarity(Rarity.LEGENDARY).build();
    public static Cosmetic SHRINKING_DEVICE = builder("shrinking_device").rarity(Rarity.LEGENDARY)
            .impl(ShrinkingDeviceImpl::new).build();
    public static Cosmetic SHONK = builder("shonk").rarity(Rarity.RARE).build();

    // Christmas Event
    public static Cosmetic CANDY_CANE = builder("candy_cane")
            .hidden()
            .rarity(Rarity.RARE)
            .tags(CosmeticTag.LIMITED_TIME)
            .build();

    public static @NotNull Cosmetic.Builder builder(@NotNull String id) {
        return new Cosmetic.Builder(CosmeticType.ACCESSORY, id)
                .impl(ModelCosmeticImpl::new);
    }
}
