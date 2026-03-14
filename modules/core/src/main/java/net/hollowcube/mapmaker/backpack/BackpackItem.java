package net.hollowcube.mapmaker.backpack;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.CustomModelData;
import net.minestom.server.network.packet.server.play.RecipeBookAddPacket;
import net.minestom.server.recipe.RecipeBookCategory;
import net.minestom.server.recipe.display.RecipeDisplay;
import net.minestom.server.recipe.display.SlotDisplay;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static net.hollowcube.mapmaker.backpack.BackpackCategory.MATERIAL;

public enum BackpackItem {
    // The ordering of this enum defines the ordering in the recipe book
    // WARNING: IF YOU CHANGE A RARITY HERE YOU MUST ALSO GO EDIT THE FILE IN RESOURCES TO CHANGE THE MAX
    //          STACK SIZE TO MATCH.
    // WARNING: IF YOU ADD OR REMOVE AN ENTRY YOU MUST ALSO GO EDIT RecipeBookHack.java TO MATCH THE SAME
    //          AMOUNT OF ENTRIES.
    // WARNING: THESE VALUES ARE MIRRORED IN THE PLAYER SERVICE AND MUST BE MAINTAINED ACROSS BOTH TO AVOID
    //          CREATING INVALID CONFIGURATIONS. WOULD PREFER TO READ THIS DATA FROM THE PLAYER SERVICE IN
    //          THE FUTURE I SUPPOSE.

    // Materials
    BRICKS(MATERIAL, Rarity.COMMON),
    CLOTH(MATERIAL, Rarity.COMMON),
    GEM(MATERIAL, Rarity.COMMON),
    GOO(MATERIAL, Rarity.COMMON),
    METAL(MATERIAL, Rarity.COMMON),
    STRING(MATERIAL, Rarity.COMMON),
    BONE_FRAGMENT(MATERIAL, Rarity.RARE),
    CONTROLLER(MATERIAL, Rarity.RARE),
    FLOWER_PETAL(MATERIAL, Rarity.RARE),
    SUGAR_CUBE(MATERIAL, Rarity.RARE),
    FIREWORK_DUST(MATERIAL, Rarity.EPIC),
    GOLD_CHUNK(MATERIAL, Rarity.EPIC),
    INFERNAL_FLAME(MATERIAL, Rarity.EPIC),
    DRAGON_SCALE(MATERIAL, Rarity.LEGENDARY),
    NIGHTMARE_FUEL(MATERIAL, Rarity.LEGENDARY),
    STARDUST(MATERIAL, Rarity.LEGENDARY),

    // Dyes

    ;

    private static final SlotDisplay CRAFTABLE = new SlotDisplay.ItemStack(RecipeBookHack.BLANK_ITEM_CRAFTABLE);
    private static final SlotDisplay UNCRAFTABLE = new SlotDisplay.ItemStack(RecipeBookHack.BLANK_ITEM_UNCRAFTABLE);

    private final int recipeBookId;
    private final BackpackCategory category;
    private final Rarity rarity;
    private final BadSprite sprite;

    BackpackItem(BackpackCategory category, Rarity rarity) {
        class IdHolder {
            static final AtomicInteger NEXT_ID = new AtomicInteger(0);
        }
        this.recipeBookId = IdHolder.NEXT_ID.getAndIncrement();

        this.category = category;
        this.rarity = rarity;

        var spriteName = String.format("cosmetic/%s/%s", category.name().toLowerCase(Locale.ROOT), name().toLowerCase(Locale.ROOT));
        this.sprite = Objects.requireNonNull(BadSprite.SPRITE_MAP.get(spriteName), spriteName);
    }

    public static @Nullable BackpackItem byId(String item) {
        try {
            return BackpackItem.valueOf(item.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public BackpackCategory category() {
        return category;
    }

    public Rarity rarity() {
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

    public int recipeBookId() {
        return recipeBookId;
    }

    public Component displayName() {
        var translationKey = "item.mapmaker." + name().toLowerCase() + ".name";
        return Objects.requireNonNullElse(
            LanguageProviderV2.translate(Component.translatable(translationKey)),
            Component.text("item.mapmaker." + name().toLowerCase() + ".name"));
    }

    public BadSprite iconSprite() {
        return BadSprite.require("icon/material/" + name().toLowerCase());
    }

    public Component iconComponent() {
        //todo the base sprite should also contain this sprite
        var iconSprite = "icon/material/" + name().toLowerCase();
        return Component.text(Objects.requireNonNull(BadSprite.SPRITE_MAP.get(iconSprite), iconSprite).fontChar()).shadowColor(ShadowColor.none());
    }

    public ItemStack getItemStack(int amount) {
        Check.argCondition(amount < 0 || amount > maxStackSize(), "amount must be between 1 and " + maxStackSize() + ", inclusive");
        var translationKeyBase = "item.mapmaker." + name().toLowerCase();
        var lore = new ArrayList<Component>();
        lore.add(rarity.asComponent());
        lore.add(Component.empty());
        lore.addAll(LanguageProviderV2.translateMulti(translationKeyBase + ".lore", List.of()));
        return ItemStack.builder(Material.STICK)
            .set(DataComponents.ITEM_MODEL, sprite.model())
            .set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of((float) amount), List.of(), List.of(), List.of()))
            .set(DataComponents.CUSTOM_NAME, displayName())
            .set(DataComponents.LORE, lore)
            .build();
    }

    RecipeBookAddPacket.Entry getRecipeBookEntry(int amount) {
        RecipeDisplay display = new RecipeDisplay.CraftingShapeless(
            List.of(amount == 0 ? UNCRAFTABLE : CRAFTABLE),
            new SlotDisplay.ItemStack(getItemStack(amount)),
            new SlotDisplay.Item(Material.CRAFTING_TABLE)
        );
        return new RecipeBookAddPacket.Entry(
            recipeBookId(), display, null,
            RecipeBookCategory.CRAFTING_REDSTONE,
            null, (byte) 0
        );
    }

//    DeclareRecipesPacket.DeclaredRecipe getRecipePlaceholder(int amount) {
//        return new DeclareRecipesPacket.DeclaredShapelessCraftingRecipe(
//            recipeBookId, "",
//            RecipeCategory.Crafting.REDSTONE,
//            amount == 0 ? UNCRAFTABLE : CRAFTABLE,
//            getItemStack(amount)
//        );
//    }
}
