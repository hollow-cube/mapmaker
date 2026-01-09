package net.hollowcube.mapmaker.editor.hdb.gui;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.responses.HeadDbSearchResponse;
import net.hollowcube.mapmaker.panels.*;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;
import static net.hollowcube.mapmaker.panels.AbstractAnvilView.simpleAnvil;

@NotNullByDefault
public class HdbBrowserPanel extends Panel {

    private final MapService maps;
    private final Pagination<SearchParams> pagination;
    private final Text searchTextElement;
    private final RadioSelect<String> categories;

    private boolean initializing = true;
    private SearchParams params;

    public HdbBrowserPanel(MapService maps) {
        this(maps, "");
    }

    public HdbBrowserPanel(MapService maps, String initialQuery) {
        super(9, 9);
        this.maps = maps;
        this.params = new SearchParams(initialQuery, null);

        background("generic2/containers/searchable/extended/7x3", -10, -31);
        add(0, 0, title("Head Database"));
        add(0, 6, new Text(9, 1, FontUtil.rewrite("small", "categories")).align(Text.CENTER, 6));

        add(0, 0, backOrClose());

        this.searchTextElement = add(1, 0, new Text(8, 1, "Search...").align(8, 5));
        this.searchTextElement.text(this.params.query.isEmpty() ? "Search..." : this.params.query);
        this.searchTextElement.onLeftClick(() -> this.host.pushView(simpleAnvil(
            "generic2/anvil/field_container",
            "map_browser/search_anvil_icon",
            "Search Heads",
            this::setQuery
        )));

        this.pagination = add(1, 2, new Pagination<>(7, 3));
        this.pagination.fetchAsync(this::onSearch);
        add(2, 5, this.pagination.prevButton());
        add(3, 5, this.pagination.pageText(3, 1));
        add(6, 5, this.pagination.nextButton());

        this.categories = this.add(2, 7, new RadioSelect<>(5, 2));
        this.categories.onChange(this::setCategory);

        HdbCategories.CATEGORIES.forEach((category, icon) -> {
            var button = this.categories.addOption(category, RadioSelect.ButtonUpdater.SQUARE_BACKGROUND);
            button.from(icon);
            button.text(Component.translatable("hdb.category." + category + ".name"), List.of());
        });
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        if (this.initializing) {
            this.pagination.reset(this.params);
            this.initializing = false;
        }
    }

    @Blocking
    private List<? extends Element> onSearch(SearchParams params, int page, int pageSize) {
        var heads = params.search(maps, page, pageSize);
        if (heads.pages() != null && heads.pages() > 0) {
            this.pagination.totalPages(heads.pages() + 1);
        }

        return heads
            .results()
            .stream()
            .map(head -> new Button(1, 1)
                .from(head.createItemStack())
                .onRightClick(() -> PlayerUtil.giveItem(this.host.player(), head.createItemStack()))
                .onLeftClick(() -> {
                    PlayerUtil.giveItem(host.player(), head.createItemStack());
                    this.host.close();
                })
            )
            .limit(pageSize)
            .toList();
    }

    private void setQuery(String query) {
        this.params = new SearchParams(query, null);
        this.searchTextElement.text(this.params.query.isEmpty() ? "Search..." : this.params.query);
        this.categories.setSelected(null);
        this.pagination.reset(this.params);
    }

    private void setCategory(@Nullable String category) {
        if (category == null) return;

        this.params = new SearchParams("", category);
        this.searchTextElement.text(this.params.query.isEmpty() ? "Search..." : this.params.query);
        this.pagination.reset(this.params);
    }

    public record SearchParams(String query, @Nullable String category) {

        public HeadDbSearchResponse search(MapService maps, int page, int pageSize) {
            if (category != null) {
                return maps.getHeadsWithCategory(category, page, pageSize);
            }
            return maps.getHeadsWithSearch(query, page, pageSize);
        }
    }
}
