package net.hollowcube.map.entity.potion;

import com.mojang.serialization.Codec;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public record PotionInfo(
        @NotNull String id,
        int maxLevel,
        @NotNull PotionEffect vanillaEffect,
        @NotNull ItemStack icon,
        int index
) implements Comparable<PotionInfo> {
    // No effect potions which we can use for custom ones:
    //  instant health, instant damage, bad omen, saturation (?), glowing, hero of the village
    // sort of (people may want to use them in maps):
    //  luck, resistance, fire resistance, invisibility, health boost (?)
    // currently used ones:
    //  bad omen

    private static int idCounter = 0;
    private static final Map<String, PotionInfo> effects = new HashMap<>();

    public static @NotNull PotionInfo getById(@NotNull String id) {
        return effects.get(id);
    }

    public static @NotNull Collection<PotionInfo> values() {
        return effects.values();
    }

    public static @NotNull Collection<PotionInfo> sortedValues() {
        var result = new ArrayList<>(effects.values());
        result.sort(null);
        return result;
    }

    public static final Codec<PotionInfo> CODEC = Codec.STRING.xmap(PotionInfo::getById, PotionInfo::id);

    public static final PotionInfo SPEED = new PotionInfo("speed", 255, PotionEffect.SPEED, Material.SUGAR);
    public static final PotionInfo JUMP_BOOST = new PotionInfo("jump_boost", 255, PotionEffect.JUMP_BOOST, Material.RABBIT_FOOT);
    public static final PotionInfo DEPTH_STRIDER = new PotionInfo("depth_strider", 3, PotionEffect.BAD_OMEN, Material.PRISMARINE_SHARD);
    public static final PotionInfo LEVITATION = new PotionInfo("levitation", 255, PotionEffect.LEVITATION, Material.CHAINMAIL_BOOTS);
    public static final PotionInfo SLOW_FALL = new PotionInfo("slow_fall", 1, PotionEffect.SLOW_FALLING, Material.FEATHER);
    public static final PotionInfo SLOWNESS = new PotionInfo("slowness", 255, PotionEffect.SLOWNESS, Material.ANVIL);
    public static final PotionInfo BLINDNESS = new PotionInfo("blindness", 255, PotionEffect.BLINDNESS, Material.SPIDER_EYE);
    public static final PotionInfo DARKNESS = new PotionInfo("darkness", 1, PotionEffect.DARKNESS, Material.BLACK_WOOL);
    public static final PotionInfo NAUSEA = new PotionInfo("nausea", 1, PotionEffect.NAUSEA, Material.CHICKEN);

    public PotionInfo {
        Check.stateCondition(effects.containsKey(id), "Duplicate effect id: " + id);
        effects.put(id, this);
    }

    public PotionInfo(@NotNull String id, int maxLevel, @NotNull PotionEffect vanillaEffect, @NotNull Material icon) {
        this(id, maxLevel, vanillaEffect, ItemStack.builder(icon)
                .displayName(Component.translatable("gui.effect.potion.type." + id + ".name"))
                .lore(LanguageProviderV2.translateMulti("gui.effect.potion.type." + id + ".lore", List.of()))
                .build(), idCounter++);
    }

    @Override
    public int compareTo(@NotNull PotionInfo o) {
        return Integer.compare(index(), o.index());
    }
}
