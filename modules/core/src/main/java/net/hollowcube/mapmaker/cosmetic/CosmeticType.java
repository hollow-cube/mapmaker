package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.player.PlayerSetting;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.Equippable;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.inventory.PlayerInventoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum CosmeticType {
    // Note: The order here is relevant to the cosmetic selector GUI.
    // Be careful when changing it.

    HAT("hat", PlayerInventoryUtils.HELMET_SLOT, "empty_helmet", EquipmentSlot.HELMET),
    BACKWEAR("backwear", PlayerInventoryUtils.CHESTPLATE_SLOT, "empty_chestplate", EquipmentSlot.CHESTPLATE),
    ACCESSORY("accessory", PlayerInventoryUtils.OFFHAND_SLOT, null, null), // Do not add empty icon for accessory because it would force you to always see it in hand.
    PET("pet", PlayerInventoryUtils.LEGGINGS_SLOT, "empty_leggings", EquipmentSlot.LEGGINGS),
    EMOTE("emote", PlayerInventoryUtils.CRAFT_SLOT_1, "empty_emote", null),
    PARTICLE("particle", PlayerInventoryUtils.BOOTS_SLOT, "empty_boots", EquipmentSlot.BOOTS),
    VICTORY_EFFECT("victory_effect", PlayerInventoryUtils.CRAFT_SLOT_2, "empty_victory_effect", null),
    ;

    public static final CosmeticType[] VALUES = values();

    public static @Nullable CosmeticType byIconSlot(int slot) {
        for (var type : VALUES) {
            if (type.iconSlot == slot) {
                return type;
            }
        }
        return null;
    }

    private final String id;
    private final PlayerSetting<String> setting;
    private final Tag<CosmeticType> tag;
    private final int iconSlot;
    private final ItemStack blankIcon;

    CosmeticType(String id, int iconSlot, String emptyIcon, @Nullable EquipmentSlot equipmentSlot) {
        this.id = id;
        this.setting = PlayerSetting.String("cosmetic." + id, "");
        this.tag = Tag.Transient("cosmetic." + id);
        this.iconSlot = iconSlot;
        var baseTranslation = "cosmetic.type." + id + ".blank";

        Tag<Boolean> COSMETIC_TAG = Tag.Boolean("cosmetic");
        if (emptyIcon == null) {
            this.blankIcon = ItemStack.AIR;
        } else {
            var builder = ItemStack.builder(Material.DIAMOND)
                    .set(ItemComponent.CUSTOM_MODEL_DATA, BadSprite.require("icon/inventory/" + emptyIcon).cmd())
                    .set(ItemComponent.CUSTOM_NAME, Component.translatable(baseTranslation + ".name"))
                    .set(ItemComponent.LORE, LanguageProviderV2.translateMulti(baseTranslation + ".lore", List.of()));
            if (equipmentSlot != null) {
                builder.set(ItemComponent.EQUIPPABLE, new Equippable(equipmentSlot, SoundEvent.ITEM_ARMOR_EQUIP_GENERIC,
                        null, null, null, false, false, false));
            }
            this.blankIcon = builder.build().withTag(COSMETIC_TAG, true);
        }
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull PlayerSetting<String> setting() {
        return setting;
    }

    public @NotNull Tag<CosmeticType> tag() {
        return tag;
    }

    public int iconSlot() {
        return iconSlot;
    }

    public @NotNull ItemStack blankIcon() {
        return blankIcon;
    }
}
