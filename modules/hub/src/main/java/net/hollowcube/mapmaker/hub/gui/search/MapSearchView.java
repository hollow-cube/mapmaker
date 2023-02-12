package net.hollowcube.mapmaker.hub.gui.search;

import net.hollowcube.canvas.section.ClickHandler;
import net.hollowcube.canvas.section.ParentSection;
import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.section.std.ButtonSection;
import net.hollowcube.canvas.section.std.GroupSection;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.hub.gui.common.BackOrCloseButton;
import net.hollowcube.mapmaker.hub.gui.common.FuturePaginationSection;
import net.hollowcube.mapmaker.hub.gui.common.InfoButton;
import net.hollowcube.mapmaker.hub.gui.common.TranslatedButtonSection;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MapSearchView extends ParentSection {
    private final FuturePaginationSection pagination;

    public MapSearchView() {
        super(9, 9);

        // Header
        add(0, 0, new BackOrCloseButton());
        add(3, 0, new TranslatedButtonSection(3, 1, "gui.map_search.title", List.of(), Material.PAPER, this::doReset));
        add(8, 0, new InfoButton("gui.map_search.info"));

        // Page area
        pagination = add(1, 1, new FuturePaginationSection(7, 5, this::getPage));
        add(0, 3, pagination.prevPageButton());
        add(8, 3, pagination.nextPageButton());

        // Filter/sort section
        add(1, 7, new ButtonSection(7, 1, ItemStack.of(Material.COMPARATOR).withDisplayName(Component.text("Sort/filter todo"))));
    }

    private boolean doReset(@NotNull Player player, int i, @NotNull ClickType clickType) {
        pagination.reset();
        return ClickHandler.DENY;
    }

    private @NotNull FutureResult<FuturePaginationSection.@Nullable PageData> getPage(int width, int height, int pageNumber) {
        int pageSize = width * height;
        // Request pageSize + 1 entries every time to see if there is at least one entry following this page.
        // If there is, then there must be another page.
        return getContext(MapStorage.class).getLatestMaps(pageNumber * pageSize, pageSize + 1)
                .map(entries -> {
                    if (entries.isEmpty())
                        return new FuturePaginationSection.PageData(null, false);

                    var page = new GroupSection(width, height);
                    for (int i = 0; i < Math.min(entries.size(), pageSize); i++) {
                        page.add(i % width, i / width, new MapEntry(entries.get(i)));
                    }
                    return new FuturePaginationSection.PageData(page, entries.size() == pageSize + 1);
                });
    }

    private static class MapEntry extends ButtonSection {
        private final MapData map;

        public MapEntry(@NotNull MapData map) {
            super(1, 1, ItemStack.AIR);
            this.map = map;

            setItem(buildItemStack());
            setOnClick(this::handleClick);
        }

        private void handleClick() {
            var router = find(RouterSection.class);
            //todo push play map view
            router.push(new PlayMapView(map));
        }

        private @NotNull ItemStack buildItemStack() {
            //todo
            return ItemStack.of(Material.ENCHANTING_TABLE)
                    .withDisplayName(Component.text(map.getName()));
        }
    }

}
