package net.hollowcube.mapmaker.map.entity.potion;

import it.unimi.dsi.fastutil.ints.Int2DoubleFunction;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.codec.Codec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.potion.PotionEffect;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public record PotionInfo(
        String id,
        int maxLevel,
        PotionEffect vanillaEffect,
        ItemStack icon,
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

    public static @Nullable PotionInfo getById(String id) {
        return effects.get(id);
    }

    public static @Nullable PotionInfo getByVanillaEffect(PotionEffect effect) {
        return effectsByVanilla.get(effect);
    }

    public static Collection<PotionInfo> values() {
        return effects.values();
    }

    public static List<PotionInfo> sortedValues() {
        var result = new ArrayList<>(effects.values());
        result.sort(null);
        return result;
    }

    static final int MAX_POTION_LEVEL = 128;

    public static final Codec<PotionInfo> CODEC = Codec.STRING.transform(PotionInfo::getById, PotionInfo::id);

    public static final PotionInfo SPEED = builder("speed").maxLevel().setVanillaEffect(PotionEffect.SPEED).setIcon("effect/potion/icon/speed")
            .attribute(Attribute.MOVEMENT_SPEED, "minecraft:effect.speed", level -> 0.2 * (level + 1), AttributeOperation.ADD_MULTIPLIED_TOTAL).build();
    public static final PotionInfo JUMP_BOOST = builder("jump_boost").maxLevel().setVanillaEffect(PotionEffect.JUMP_BOOST).setIcon("effect/potion/icon/jump_boost")
            .attribute(Attribute.SAFE_FALL_DISTANCE, "minecraft:effect.jump_boost", level -> level + 1, AttributeOperation.ADD_VALUE).build();
    public static final PotionInfo DEPTH_STRIDER = builder("depth_strider").maxLevel(3).setVanillaEffect(PotionEffect.BAD_OMEN).setIcon("effect/potion/icon/depth_strider").setHandler(new DepthStriderPotionHandler()).build();
    public static final PotionInfo LEVITATION = builder("levitation").maxLevel().setVanillaEffect(PotionEffect.LEVITATION).setIcon("effect/potion/icon/levitation").build();
    public static final PotionInfo SLOW_FALL = builder("slow_fall").setVanillaEffect(PotionEffect.SLOW_FALLING).setIcon("effect/potion/icon/slow_fall").build();

    // .addAttributeModifier(Attributes.SAFE_FALL_DISTANCE, ResourceLocation.withDefaultNamespace("effect.jump_boost"), 1.0, AttributeModifier.Operation.ADD_VALUE));

    public static final PotionInfo SLOWNESS = builder("slowness").maxLevel().setVanillaEffect(PotionEffect.SLOWNESS).setIcon("effect/potion/icon/slowness")
            .attribute(Attribute.MOVEMENT_SPEED, "minecraft:effect.slowness", level -> -0.15f * (level + 1), AttributeOperation.ADD_MULTIPLIED_TOTAL).build();
    public static final PotionInfo BLINDNESS = builder("blindness").maxLevel().setVanillaEffect(PotionEffect.BLINDNESS).setIcon("effect/potion/icon/blindness").build();
    public static final PotionInfo DARKNESS = builder("darkness").setVanillaEffect(PotionEffect.DARKNESS).setIcon("effect/potion/icon/darkness").build();
    public static final PotionInfo NAUSEA = builder("nausea").setVanillaEffect(PotionEffect.NAUSEA).setIcon("effect/potion/icon/nausea").build();
    public static final PotionInfo DOLPHINS_GRACE = builder("dolphins_grace").setVanillaEffect(PotionEffect.DOLPHINS_GRACE).setIcon("effect/potion/icon/dolphins_grace").build();
    public static final PotionInfo SWIFT_SNEAK = builder("swift_sneak").maxLevel(5).setVanillaEffect(PotionEffect.INFESTED).setIcon("effect/potion/icon/swift_sneak")
            .attribute(Attribute.SNEAKING_SPEED, "minecraft:enchantment.swift_sneak", level -> 0.15 * (level + 1), AttributeOperation.ADD_VALUE).build();

    public String translationKey() {
        return "gui.effect.potion.type." + id;
    }

    public String itemModel() {
        return Objects.requireNonNull(this.icon.get(DataComponents.ITEM_MODEL));
    }

    @Override
    public int compareTo(PotionInfo o) {
        return Integer.compare(index(), o.index());
    }

    private static Builder builder(String id) {
        return new Builder(id);
    }

    private static class Builder {
        private final String id;
        private int maxLevel = 1;
        private @Nullable PotionEffect vanillaEffect = null;
        private @Nullable ItemStack icon = null;
        private @Nullable PotionHandler handler;
        private final int index = idCounter++;

        private Builder(String id) {
            this.id = id;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder maxLevel() {
            return maxLevel(MAX_POTION_LEVEL);
        }

        public Builder setVanillaEffect(PotionEffect vanillaEffect) {
            this.vanillaEffect = vanillaEffect;
            return this;
        }

        public Builder setIcon(Material icon) {
            return setIcon(ItemStack.builder(icon));
        }

        public Builder setIcon(String spritePath) {
            var sprite = BadSprite.SPRITE_MAP.get(spritePath);
            if (sprite == null) {
                setIcon(ItemStack.builder(Material.DIAMOND));
                return this;
            }
            return setIcon(ItemStack.builder(Material.DIAMOND)
                    .set(DataComponents.ITEM_MODEL, Objects.requireNonNull(sprite.model(), "Sprite model not set")));
        }

        public Builder setIcon(ItemStack.Builder icon) {
            this.icon = icon
                    .set(DataComponents.CUSTOM_NAME, Component.translatable("gui.effect.potion.type." + id + ".name"))
                    .set(DataComponents.LORE, LanguageProviderV2.translateMulti("gui.effect.potion.type." + id + ".lore", List.of()))
                    .build();
            return this;
        }

        public Builder setHandler(@Nullable PotionHandler handler) {
            this.handler = handler;
            return this;
        }

        public Builder attribute(Attribute attribute, String id, Int2DoubleFunction formula, AttributeOperation operation) {
            this.handler = new GenericModifierPotionHandler(attribute, Key.key(id), formula, operation);
            return this;
        }

        public PotionInfo build() {
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
