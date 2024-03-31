package net.hollowcube.mapmaker.backpack;

import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The client puts all the recipes in a {@link HashSet} of 'RecipeEntry' which has a {@link #hashCode()} defined
 * by the hashcode of the recipe namespace ID. This means that we can sort the recipe book as long as we get IDs that
 * result in the same order when put into a {@link HashSet}. Unfortunately hashset ordering has no guarantees, however
 * I have found that if we use the exact same number of entries it is consistent.
 */
public class RecipeBookHack {
    // THE FOLLOWING MUST BE KEPT UP TO DATE WITH THEIR RESPECTIVE ENUMS
    private static final int TOTAL_CRAFTING_MATERIALS = 8;
    private static final int MAX_IDS = TOTAL_CRAFTING_MATERIALS;
    private static final List<String> orderedIds;

    public static final ItemStack BLANK_ITEM_CRAFTABLE = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(1))
            // If this has a name it no longer matches in recipes
//            .displayName(Component.text(""))
            .build();
    public static final ItemStack BLANK_ITEM_UNCRAFTABLE = ItemStack.of(Material.GLOWSTONE_DUST);

    /**
     * Returns a namespace ID for a recipe with the given ordering index.
     *
     * @param index The index, must be between 0 and {@link #MAX_IDS}, inclusive.
     * @return The namespace ID.
     */
    public static @NotNull String getOrderedId(int index) {
        Check.argCondition(index < 0 || index >= MAX_IDS, "Index out of bounds");
        return orderedIds.get(index);
    }

    static {
        var setForSorting = new HashSet<Key>();
        for (int i = 0; i < MAX_IDS; i++) {
            setForSorting.add(Key.key("a:i" + i));
        }

        var sorted = new ArrayList<String>();
        for (var hash : setForSorting) {
            sorted.add(hash.asString());
        }

        orderedIds = List.copyOf(sorted);
    }

}
