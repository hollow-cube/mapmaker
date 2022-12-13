package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.canvas.std.GroupSection;
import net.hollowcube.canvas.std.PaginationSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class SearchDemo extends ParentSection {
    public static final int ENTRIES = 100;
    public static final int PAGE_SIZE = 7 * 5;

    private GroupSection basicOptions = null;
    private GroupSection advancedOptions = null;
    private boolean optionsToggle = false; // false = basic, true = advanced

    public SearchDemo() {
        super(9, 9);

        // Back button (or exit)
        add(0, new ButtonSection(1, 1, ItemStack.of(Material.PLAYER_HEAD), () -> {
        }));
        // Info button
        add(8, new ButtonSection(1, 1, ItemStack.of(Material.PLAYER_HEAD), () -> {
        }));

        // Title bar
        add(3, new ButtonSection(3, 1, ItemStack.of(Material.PAPER), () -> {
        }));

        var pagination = add(1, 1, new PaginationSection(7, 5, this::getPage));
        add(0, 3, pagination.lastPageButton(1, 1));
        add(8, 3, pagination.nextPageButton(1, 1));

        // Controls

        // Search
        add(8, 7, new ButtonSection(1, 1, ItemStack.of(Material.OAK_SIGN), () -> {
        }));

        // Advanced mode toggle
        add(0, 7, new ButtonSection(1, 1, ItemStack.of(Material.COMPASS), () -> {
            if (optionsToggle) {
                // Advanced->Basic
                unmountChild(1, 6, advancedOptions);
                mountChild(1, 6, basicOptions);
            } else {
                // Basic->Advanced
                unmountChild(1, 6, basicOptions);
                mountChild(1, 6, advancedOptions);
            }
            optionsToggle = !optionsToggle;
        }));

        basicOptions = new GroupSection(7, 2);
        basicOptions.add(1, 1, new ButtonSection(1, 1, ItemStack.of(Material.EMERALD), () -> {}));
        basicOptions.add(2, 1, new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND), () -> {}));
        basicOptions.add(3, 1, new ButtonSection(1, 1, ItemStack.of(Material.IRON_SWORD), () -> {}));
        basicOptions.add(4, 1, new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND_BOOTS), () -> {}));
        basicOptions.add(5, 1, new ButtonSection(1, 1, ItemStack.of(Material.BRICK), () -> {}));

        advancedOptions = new GroupSection(7, 2);
        advancedOptions.add(1, 1, new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND), () -> {}));
        advancedOptions.add(2, 1, new ButtonSection(1, 1, ItemStack.of(Material.COMPARATOR), () -> {}));
        advancedOptions.add(3, 1, new ButtonSection(1, 1, ItemStack.of(Material.HOPPER), () -> {}));
        advancedOptions.add(4, 1, new ButtonSection(1, 1, ItemStack.of(Material.NAME_TAG), () -> {}));
        advancedOptions.add(5, 1, new ButtonSection(1, 1, ItemStack.of(Material.WRITABLE_BOOK), () -> {}));

        // Add basic options for now
        //todo once again problems would be solved by re adding that out of bounds check
        add(1, 6, basicOptions);
    }

    private @Nullable Section getPage(int pageNumber) {
        if (pageNumber > ENTRIES / PAGE_SIZE)
            return null;
        var page = new GroupSection(7, 5);
        IntStream.range(pageNumber * PAGE_SIZE, (pageNumber + 1) * PAGE_SIZE)
                .filter(i -> i < ENTRIES)
                .forEach(i -> {
                    var x = (i - pageNumber * PAGE_SIZE) % 7;
                    var y = (i - pageNumber * PAGE_SIZE) / 7;
                    var item = ItemStack.builder(Material.ENCHANTING_TABLE)
                            .displayName(Component.text("Entry " + i)).build();
                    page.add(x, y, new ButtonSection(1, 1, item, () -> {
                        System.out.println("Clicked entry " + i);
                    }));
                });
        return page;
    }

}
