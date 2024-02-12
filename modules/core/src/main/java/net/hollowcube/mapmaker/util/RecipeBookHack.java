package net.hollowcube.mapmaker.util;

import net.kyori.adventure.key.Key;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RecipeBookHack {
    // THE FOLLOWING MUST BE KEPT UP TO DATE WITH THEIR RESPECTIVE ENUMS
    private static final int TOTAL_CRAFTING_MATERIALS = 16;
    private static final int MAX_IDS = TOTAL_CRAFTING_MATERIALS;
    private static final List<String> orderedIds;

    public static final ItemStack BLANK_ITEM = ItemStack.builder(Material.STICK)
            .meta(meta -> meta.customModelData(1))
            // If this has a name it no longer matches in recipes
//            .displayName(Component.text(""))
            .build();

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
