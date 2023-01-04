package net.hollowcube.canvas.demo;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.canvas.std.GroupSection;
import net.hollowcube.canvas.std.PaginationSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.IntStream;

public class SearchDemo extends ParentSection {
    public static final int ENTRIES = 100;
    public static final int PAGE_SIZE = 7 * 5;

    private final Section basicOptions = new BasicOptions();
    private final Section advancedOptions = new AdvancedOptions();
    private boolean optionsToggle = false; // false = basic, true = advanced

    public SearchDemo() {
        super(9, 9);

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

        // Add basic options for now
        //todo once again problems would be solved by re adding that out of bounds check
        add(1, 6, basicOptions);
    }

    @Override
    protected void mount() {
        super.mount();
        // Add back button if there is a history, otherwise add exit button
        if (find(RouterSection.class).hasHistory()) {
            add(0, new ButtonSection(1, 1, ItemStack.of(Material.ARROW), () -> {
                var router = find(RouterSection.class);
                if (router == null) return;
                router.pop();
            }));
        } else {
            add(0, new ButtonSection(1, 1, ItemStack.of(Material.BARRIER), () -> {
                System.out.println("Exit");
            }));
        }
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

    public static class BasicOptions extends ParentSection {

        public BasicOptions() {
            super(7, 2);

            add(1, 1, new ButtonSection(1, 1, ItemStack.of(Material.EMERALD), () -> {}));
            add(2, 1, new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND), () -> {}));
            add(3, 1, new ButtonSection(1, 1, ItemStack.of(Material.IRON_SWORD), () -> {}));
            add(4, 1, new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND_BOOTS), () -> {}));
            add(5, 1, new ButtonSection(1, 1, ItemStack.of(Material.BRICK), () -> {}));
        }

    }

    public static class AdvancedOptions extends ParentSection {

        private record SubOption(ButtonSection button, Section section) {}
        private final SubOption[] subOptions;
        private int selected = 0;

        public AdvancedOptions() {
            super(7, 2);

            subOptions = new SubOption[]{
                    new SubOption(
                            new ButtonSection(1, 1, ItemStack.of(Material.DIAMOND), () -> selectSubOptions(0)),
                            new QualityOption()
                    ),
                    new SubOption(
                            new ButtonSection(1, 1, ItemStack.of(Material.COMPARATOR), () -> selectSubOptions(1)),
                            new TagOption()
                    ),
                    new SubOption(
                            new ButtonSection(1, 1, ItemStack.of(Material.HOPPER), () -> selectSubOptions(2)),
                            new TempSubOption(2)
                    ),
                    new SubOption(
                            new ButtonSection(1, 1, ItemStack.of(Material.NAME_TAG), () -> selectSubOptions(3)),
                            new TempSubOption(3)
                    ),
                    new SubOption(
                            new ButtonSection(1, 1, ItemStack.of(Material.WRITABLE_BOOK), () -> selectSubOptions(4)),
                            new TempSubOption(4)
                    ),
            };

            for (int i = 0; i < subOptions.length; i++) {
                add(i + 1, 1, subOptions[i].button);
            }

            subOptions[0].button.setItem(subOptions[0].button.getItem().withAmount(2));
            add(0, 0, subOptions[0].section);
        }

        private void selectSubOptions(int index) {
            if (index == selected) return;
            unmountChild(0, 0, subOptions[selected].section);
            for (int i = 0; i < subOptions.length; i++) {
                var subOption = subOptions[i];
                if (i == index) {
                    // Select this button
                    subOption.button.setItem(subOption.button.getItem().withAmount(2));
                    mountChild(0, 0, subOption.section);
                } else {
                    subOption.button.setItem(subOption.button.getItem().withAmount(1));
                }
            }
            selected = index;
        }

        private class TempSubOption extends ParentSection {
            public TempSubOption(int i) {
                super(7, 1);
                add(i, 0, new ButtonSection(1, 1, ItemStack.of(Material.BRICK), () -> {}));
            }
        }

        private class QualityOption extends ParentSection {
            public QualityOption() {
                super(7, 1);

                add(1, new ToggleButton(ItemStack.of(Material.CLAY, 2), ItemStack.of(Material.CLAY)));
                add(2, new ToggleButton(ItemStack.of(Material.DIAMOND, 2), ItemStack.of(Material.DIAMOND)));
                add(3, new ToggleButton(ItemStack.of(Material.GOLD_INGOT, 2), ItemStack.of(Material.GOLD_INGOT)));
                add(4, new ToggleButton(ItemStack.of(Material.ENDER_EYE, 2), ItemStack.of(Material.ENDER_EYE)));
                add(5, new ToggleButton(ItemStack.of(Material.PRISMARINE_SHARD, 2), ItemStack.of(Material.PRISMARINE_SHARD)));
            }
        }

        private class TagOption extends ParentSection {
            public static final int ENTRIES = 12;
            public static final int PAGE_SIZE = 5;

            public TagOption() {
                super(7, 1);

                var pagination = add(1, new PaginationSection(5, 1, this::getPage));
                add(0, pagination.lastPageButton(1, 1));
                add(6, pagination.nextPageButton(1, 1));
            }

            private @Nullable Section getPage(int pageNumber) {
                if (pageNumber > ENTRIES / PAGE_SIZE)
                    return null;
                var page = new GroupSection(5, 1);
                IntStream.range(pageNumber * PAGE_SIZE, (pageNumber + 1) * PAGE_SIZE)
                        .filter(i -> i < ENTRIES)
                        .forEach(i -> {
                            var x = (i - pageNumber * PAGE_SIZE) % 7;
                            var y = (i - pageNumber * PAGE_SIZE) / 7;
                            var item = ItemStack.builder(Material.fromId(i + 25)).build();
                            page.add(x, y, new ToggleButton(item.withAmount(2), item));
                        });
                return page;
            }
        }



    }

    private static class ToggleButton extends ButtonSection {
        private final ItemStack on;
        private final ItemStack off;
        private boolean state = true;

        public ToggleButton(ItemStack on, ItemStack off) {
            super(1, 1, on, () -> {});
            this.on = on;
            this.off = off;
        }

        @Override
        public @NotNull ItemStack getItem() {
            return state ? on : off;
        }

        @Override
        protected boolean handleClick(int slot, @NotNull Player player, @NotNull ClickType clickType) {
            state = !state;
            update();
            return false;
        }

        private void update() {
            setItem(getItem());
        }
    }

}
