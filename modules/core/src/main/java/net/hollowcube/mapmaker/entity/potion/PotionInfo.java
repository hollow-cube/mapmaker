package net.hollowcube.mapmaker.entity.potion;

import com.mojang.serialization.Codec;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record PotionInfo(
        @NotNull String id,
        int maxLevel,
        @NotNull PotionEffect vanillaEffect,
        @NotNull ItemStack icon,
        @Nullable PotionHandler handler,
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
    private static final Map<PotionEffect, PotionInfo> effectsByVanilla = new HashMap<>();

    public static @Nullable PotionInfo getById(@NotNull String id) {
        return effects.get(id);
    }

    public static @Nullable PotionInfo getByVanillaEffect(@NotNull PotionEffect effect) {
        return effectsByVanilla.get(effect);
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

    public static final PotionInfo SPEED = builder("speed").maxLevel(255).setVanillaEffect(PotionEffect.SPEED).setIcon(Material.SUGAR).setHandler(new SpeedPotionHandler()).build();
    public static final PotionInfo JUMP_BOOST = builder("jump_boost").maxLevel(255).setVanillaEffect(PotionEffect.JUMP_BOOST).setIcon(Material.RABBIT_FOOT).build();
    public static final PotionInfo DEPTH_STRIDER = builder("depth_strider").maxLevel(3).setVanillaEffect(PotionEffect.BAD_OMEN).setIcon(Material.PRISMARINE_SHARD).setHandler(new DepthStriderPotionHandler()).build();
    public static final PotionInfo LEVITATION = builder("levitation").maxLevel(255).setVanillaEffect(PotionEffect.LEVITATION).setIcon(Material.CHAINMAIL_BOOTS).build();
    public static final PotionInfo SLOW_FALL = builder("slow_fall").setVanillaEffect(PotionEffect.SLOW_FALLING).setIcon(Material.FEATHER).build();
    public static final PotionInfo SLOWNESS = builder("slowness").maxLevel(255).setVanillaEffect(PotionEffect.SLOWNESS).setIcon(Material.ANVIL).build();
    public static final PotionInfo BLINDNESS = builder("blindness").maxLevel(255).setVanillaEffect(PotionEffect.BLINDNESS).setIcon(Material.SPIDER_EYE).build();
    public static final PotionInfo DARKNESS = builder("darkness").setVanillaEffect(PotionEffect.DARKNESS).setIcon(Material.BLACK_WOOL).build();
    public static final PotionInfo NAUSEA = builder("nausea").setVanillaEffect(PotionEffect.NAUSEA).setIcon(Material.CHICKEN).build();

    @Override
    public int compareTo(@NotNull PotionInfo o) {
        return Integer.compare(index(), o.index());
    }

    private static @NotNull Builder builder(@NotNull String id) {
        return new Builder(id);
    }

    private static class Builder {
        private final String id;
        private int maxLevel = 1;
        private PotionEffect vanillaEffect = null;
        private ItemStack icon = null;
        private PotionHandler handler;
        private final int index = idCounter++;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        public @NotNull Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public @NotNull Builder setVanillaEffect(PotionEffect vanillaEffect) {
            this.vanillaEffect = vanillaEffect;
            return this;
        }

        public @NotNull Builder setIcon(@NotNull Material icon) {
            this.icon = ItemStack.builder(icon)
                    .displayName(Component.translatable("gui.effect.potion.type." + id + ".name"))
                    .lore(LanguageProviderV2.translateMulti("gui.effect.potion.type." + id + ".lore", List.of()))
                    .build();
            return this;
        }

        public @NotNull Builder setHandler(@Nullable PotionHandler handler) {
            this.handler = handler;
            return this;
        }

        public @NotNull PotionInfo build() {
            Check.stateCondition(effects.containsKey(id), "Duplicate effect id: " + id);
            Check.stateCondition(vanillaEffect == null, "Vanilla effect not set");
            Check.stateCondition(effectsByVanilla.containsKey(vanillaEffect), "Duplicate vanilla effect: " + vanillaEffect);
            Check.stateCondition(icon == null, "Icon not set");
            var result = new PotionInfo(id, maxLevel, vanillaEffect, icon, handler, index);
            effects.put(id, result);
            effectsByVanilla.put(vanillaEffect, result);
            return result;
        }

    }
}
