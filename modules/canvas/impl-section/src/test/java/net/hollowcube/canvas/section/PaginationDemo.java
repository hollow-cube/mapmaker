package net.hollowcube.canvas.section;

import net.hollowcube.canvas.section.std.ButtonSection;
import net.hollowcube.canvas.section.std.IconSection;
import net.hollowcube.canvas.section.std.PaginationSection;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class PaginationDemo extends ParentSection {

    public PaginationDemo() {
        super(9, 6);

        // Setup pagination window in center 7x4
        var pagination = add(1, 1, new PaginationSection(7, 4, this::createPage));
        //todo add check that `add` call does not overlap another

        add(1, 5, pagination.lastPageButton(3, 1));
        add(4, 5, new PageSection(pagination));
        add(5, 5, pagination.nextPageButton(3, 1));
    }

    private @Nullable Section createPage(int page) {
        if (page > 8) return null;

        var material = Material.fromId(Material.WHITE_STAINED_GLASS.id() + page);
        return new ButtonSection(7, 4, ItemStack.of(material), () -> {
            System.out.println("Clicked page " + page);
        });
    }

    private static class PageSection extends IconSection implements Consumer<PaginationSection> {
        private final PaginationSection pagination;

        public PageSection(@NotNull PaginationSection pagination) {
            super(ItemStack.of(Material.PAPER));
            this.pagination = pagination;
            accept(pagination);
        }

        @Override
        protected void mount() {
            super.mount();
            pagination.addUpdateHandler(this);
        }

        @Override
        protected void unmount() {
            super.unmount();
            pagination.addUpdateHandler(this);
        }

        @Override
        public void accept(PaginationSection paginationComponent) {
            setItem(ItemStack.builder(Material.PAPER)
                    .displayName(net.kyori.adventure.text.Component.text("Page " + paginationComponent.getPage()))
                    .build());
        }
    }

}
