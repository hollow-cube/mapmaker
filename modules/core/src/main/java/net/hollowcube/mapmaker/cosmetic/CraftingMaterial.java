package net.hollowcube.mapmaker.cosmetic;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.RecipeBookHack;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.DeclareRecipesPacket;
import net.minestom.server.recipe.RecipeCategory;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public enum CraftingMaterial {
    // The ordering of this enum defines the ordering of the crafting material in the recipe book
    // WARNING: IF YOU CHANGE A RARITY HERE YOU MUST ALSO GO EDIT THE FILE IN RESOURCES TO CHANGE THE MAX
    //          STACK SIZE TO MATCH.
    // WARNING: IF YOU ADD OR REMOVE AN ENTRY YOU MUST ALSO GO EDIT RecipeBookHack.java TO MATCH THE SAME
    //          AMOUNT OF ENTRIES.

    BRICKS(Rarity.COMMON),
    CLOTH(Rarity.COMMON),
    GEM(Rarity.COMMON),
    GOO(Rarity.COMMON),
    METAL(Rarity.COMMON),
    STRING(Rarity.COMMON),

    BONE_FRAGMENT(Rarity.RARE),
    CONTROLLER(Rarity.RARE),
    FLOWER_PETAL(Rarity.RARE),
    SUGAR_CUBE(Rarity.RARE),

    DRAGON_SCALES(Rarity.EPIC),
    FIREWORK_DUST(Rarity.EPIC),
    INFERNAL_FLAME(Rarity.EPIC),

    GOLD_CHUNK(Rarity.LEGENDARY),
    NIGHTMARE_FUEL(Rarity.LEGENDARY),
    STARDUST(Rarity.LEGENDARY),
    ;
    private static final int START_SORTED_ID = 0;

    private final Rarity rarity;
    private final BadSprite sprite;
    private final String recipeBookId;

    CraftingMaterial(@NotNull Rarity rarity) {
        this.rarity = rarity;
        var spriteName = String.format("cosmetic/material/%s", name().toLowerCase());
        this.sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get(spriteName), spriteName);
        this.recipeBookId = RecipeBookHack.getOrderedId(START_SORTED_ID + ordinal());
    }

    public @NotNull Rarity rarity() {
        return rarity;
    }

    public int maxStackSize() {
        return switch (rarity) {
            case COMMON -> 32;
            case RARE -> 16;
            case EPIC -> 8;
            case LEGENDARY -> 4;
        };
    }

    public @NotNull String recipeBookId() {
        return recipeBookId;
    }

    public @NotNull ItemStack getItemStack(int amount) {
        Check.argCondition(amount < 1 || amount > maxStackSize(), "amount must be between 1 and " + maxStackSize() + ", inclusive");
        var translationKeyBase = "item.mapmaker." + name().toLowerCase();
        return ItemStack.builder(Material.DIAMOND)
                .meta(meta -> meta.customModelData(sprite.cmd() + amount - 1))
                .displayName(Component.text(ordinal() + " -> " + recipeBookId))
                .displayName(Component.translatable(translationKeyBase + ".name"))
                .lore(LanguageProviderV2.translateMulti(translationKeyBase + ".lore", List.of()))
                .build();
    }

    public @NotNull DeclareRecipesPacket.DeclaredRecipe getRecipePlaceholder(int amount) {
        return new DeclareRecipesPacket.DeclaredShapelessCraftingRecipe(
                recipeBookId, "",
                RecipeCategory.Crafting.REDSTONE,
                List.of(new DeclareRecipesPacket.Ingredient(List.of(RecipeBookHack.BLANK_ITEM))),
                getItemStack(amount)
        );
    }
}
