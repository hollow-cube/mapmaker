package net.hollowcube.mapmaker.map.entity.info;

import net.hollowcube.mapmaker.map.entity.impl.base.AbstractLivingEntity;
import net.kyori.adventure.key.Key;
import net.minestom.server.color.DyeColor;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

import java.util.Locale;

public class CommonMapEntityInfoTypes {

    public static <E extends AbstractLivingEntity<?>> MapEntityInfoType<Boolean, E> Saddle() {
        return new MapEntityInfoTypes.Bool<>(
            false,
            (entity, data) -> entity.setEquipment(EquipmentSlot.SADDLE, data ? ItemStack.of(Material.SADDLE) : ItemStack.AIR),
            (entity) -> entity.getEquipment(EquipmentSlot.SADDLE).material() == Material.SADDLE
        );
    }

    public static <E extends AbstractLivingEntity<?>> MapEntityInfoType<DyeColor, E> DyeBodyArmor(String suffix) {
        // This is needed because intellij will complain that it's not null if we don't declare it directly.
        //noinspection Convert2Diamond
        return new MapEntityInfoTypes.NullableEnum<DyeColor, E>(
            DyeColor.class,
            null,
            (entity, data) -> {
                var stack = ItemStack.AIR;
                if (data != null) {
                    var key = Key.key("minecraft",  data.name().toLowerCase(Locale.ROOT) + suffix);
                    var material = Material.fromKey(key);
                    stack = material != null ? ItemStack.of(material) : ItemStack.AIR;
                }

                entity.setEquipment(EquipmentSlot.BODY, stack);
            },
            (entity) -> {
                var key = entity.getEquipment(EquipmentSlot.BODY).material().key();
                var color = key.value().endsWith(suffix) ? key.value().substring(0, key.value().length() - suffix.length()) : null;

                if (color == null) {
                    return null;
                } else {
                    try {
                        return DyeColor.valueOf(color.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException _) {
                        return null;
                    }
                }
            }
        );
    }

    public static <T extends Enum<T> & BodyArmor, E extends AbstractLivingEntity<?>> MapEntityInfoType<T, E> BodyArmor(Class<T> clazz) {
        // This is needed because intellij will complain that it's not null if we don't declare it directly.
        //noinspection Convert2Diamond
        return new MapEntityInfoTypes.NullableEnum<T, E>(
            clazz,
            null,
            (entity, data) -> entity.setEquipment(EquipmentSlot.BODY, data != null ? ItemStack.of(data.material()) : ItemStack.AIR),
            (entity) -> {
                var material = entity.getEquipment(EquipmentSlot.BODY).material();
                for (var armor : clazz.getEnumConstants()) {
                    if (armor.material() == material) {
                        return armor;
                    }
                }
                return null;
            }
        );
    }

    public interface BodyArmor {

        Material material();
    }

    public enum HorseArmor implements BodyArmor {
        LEATHER(Material.LEATHER_HORSE_ARMOR),
        COPPER(Material.COPPER_HORSE_ARMOR),
        IRON(Material.IRON_HORSE_ARMOR),
        GOLD(Material.GOLDEN_HORSE_ARMOR),
        DIAMOND(Material.DIAMOND_HORSE_ARMOR),
        NETHERITE(Material.NETHERITE_HORSE_ARMOR)
        ;

        private final Material material;

        HorseArmor(Material material) {
            this.material = material;
        }

        @Override
        public Material material() {
            return this.material;
        }
    }

    public enum NautilusArmor implements BodyArmor {
        COPPER(Material.COPPER_NAUTILUS_ARMOR),
        IRON(Material.IRON_NAUTILUS_ARMOR),
        GOLD(Material.GOLDEN_NAUTILUS_ARMOR),
        DIAMOND(Material.DIAMOND_NAUTILUS_ARMOR),
        NETHERITE(Material.NETHERITE_NAUTILUS_ARMOR)
        ;

        private final Material material;

        NautilusArmor(Material material) {
            this.material = material;
        }

        @Override
        public Material material() {
            return this.material;
        }
    }
}
