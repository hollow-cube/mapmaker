package net.hollowcube.mapmaker.hub.gui.search;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.canvas.std.GroupSection;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.hub.gui.common.BackOrCloseButton;
import net.hollowcube.mapmaker.hub.gui.common.FuturePaginationSection;
import net.hollowcube.mapmaker.hub.gui.common.InfoButton;
import net.hollowcube.mapmaker.hub.gui.common.TranslatedButtonSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.IntStream;

public class MapSearchView extends ParentSection {
    private final FuturePaginationSection pagination;

    public MapSearchView() {
        super(9, 9);

        // Header
        add(0, 0, new BackOrCloseButton());
        add(3, 0, new TranslatedButtonSection(3, 1, "gui.map_search.title", List.of(), Material.PAPER, this::doReset));
        add(8, 0, new InfoButton("gui.map_search.info"));

        pagination = add(1, 1, new FuturePaginationSection(7, 5, this::getPage));
        add(0, 3, pagination.prevPageButton());
        add(8, 3, pagination.nextPageButton());

    }

    private boolean doReset(@NotNull Player player, int i, @NotNull ClickType clickType) {
        pagination.reset();
        return ClickHandler.DENY;
    }

    public static final int ENTRIES = 100;
    public static final int PAGE_SIZE = 7 * 5;


    private @NotNull FutureResult<FuturePaginationSection.@Nullable PageData> getPage(int width, int height, int pageNumber) {
        System.out.println("FETCH PAGE: " + pageNumber);
        return FutureResult.supply(() -> {
            if (pageNumber > ENTRIES / PAGE_SIZE)
                return Result.ofNull();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var page = new GroupSection(width, height);
            IntStream.range(pageNumber * PAGE_SIZE, (pageNumber + 1) * PAGE_SIZE)
                    .filter(i -> i < ENTRIES)
                    .forEach(i -> {
                        var x = (i - pageNumber * PAGE_SIZE) % width;
                        var y = (i - pageNumber * PAGE_SIZE) / width;
                        var item = ItemStack.builder(Material.ENCHANTING_TABLE)
                                .displayName(Component.text("Entry " + i)).build();
                        page.add(x, y, new ButtonSection(1, 1, item, () -> {
                            System.out.println("Clicked entry " + i);
                        }));
                    });
            return Result.of(new FuturePaginationSection.PageData(page, pageNumber < ENTRIES / PAGE_SIZE));
        });
    }

    private void updateQuery() {
        pagination.reset();
    }

}
