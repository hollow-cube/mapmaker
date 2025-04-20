package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.LegacyAttributeRenameFix;

import java.util.Map;

public class V2523 extends DataVersion {
    private static final Map<String, String> ATTRIBUTE_IDS;

    public V2523() {
        super(2523);

        var fix = new LegacyAttributeRenameFix(ATTRIBUTE_IDS);
        addFix(DataTypes.ITEM_STACK, fix::fixInItemStack);
        addFix(DataTypes.ENTITY, fix::fixInEntity);
    }

    static {
        ATTRIBUTE_IDS = Map.ofEntries(
                Map.entry("generic.maxHealth", "minecraft:generic.max_health"),
                Map.entry("Max Health", "minecraft:generic.max_health"),
                Map.entry("zombie.spawnReinforcements", "minecraft:zombie.spawn_reinforcements"),
                Map.entry("Spawn Reinforcements Chance", "minecraft:zombie.spawn_reinforcements"),
                Map.entry("horse.jumpStrength", "minecraft:horse.jump_strength"),
                Map.entry("Jump Strength", "minecraft:horse.jump_strength"),
                Map.entry("generic.followRange", "minecraft:generic.follow_range"),
                Map.entry("Follow Range", "minecraft:generic.follow_range"),
                Map.entry("generic.knockbackResistance", "minecraft:generic.knockback_resistance"),
                Map.entry("Knockback Resistance", "minecraft:generic.knockback_resistance"),
                Map.entry("generic.movementSpeed", "minecraft:generic.movement_speed"),
                Map.entry("Movement Speed", "minecraft:generic.movement_speed"),
                Map.entry("generic.flyingSpeed", "minecraft:generic.flying_speed"),
                Map.entry("Flying Speed", "minecraft:generic.flying_speed"),
                Map.entry("generic.attackDamage", "minecraft:generic.attack_damage"),
                Map.entry("generic.attackKnockback", "minecraft:generic.attack_knockback"),
                Map.entry("generic.attackSpeed", "minecraft:generic.attack_speed"),
                Map.entry("generic.armorToughness", "minecraft:generic.armor_toughness")
        );
    }
}
