package net.hollowcube.mapmaker.editor.hdb.gui;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.editor.hdb.HeadDatabase;
import net.hollowcube.mapmaker.editor.hdb.HeadInfo;
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

    private final HeadDatabase hdb;
    private final Pagination<SearchParams> pagination;
    private final Text searchTextElement;
    private final RadioSelect<String> categories;

    private SearchParams params;

    public HdbBrowserPanel(HeadDatabase hdb) {
        this(hdb, "");
    }

    public HdbBrowserPanel(HeadDatabase hdb, String initialQuery) {
        super(9, 9);
        this.hdb = hdb;
        this.params = new SearchParams(initialQuery, null);

        background("generic2/containers/searchable/7x3e", -10, -31);
        add(0, 0, title("Head Database"));
        add(0, 6, new Text(9, 1, FontUtil.rewrite("small", "categories")).align(Text.CENTER, 6));

        add(0, 0, backOrClose());

        this.searchTextElement = add(1, 0, new Text(8, 1, "Search...").align(8, 5));
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
        for (var category : hdb.categories()) {
            var button = this.categories.addOption(category, RadioSelect.ButtonUpdater.SQUARE_BACKGROUND);
            button.from(hdb.categoryIcon(category));
            button.text(Component.translatable("hdb.category." + category + ".name"), List.of());
        }
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        this.pagination.reset(this.params);
    }

    @Blocking
    private List<? extends Element> onSearch(SearchParams params, int page, int pageSize) {
        var heads = params.search(hdb, page, pageSize);
        if (heads.size() > pageSize) {
            this.pagination.totalPages(page + 2);
        }

        return heads
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

        public List<HeadInfo> search(HeadDatabase hdb, int page, int pageSize) {
            if (category != null) {
                return hdb.getInCategory(category, page, pageSize);
            } else if (query.isEmpty()) {
                return hdb.getRandom(pageSize);
            } else {
                return hdb.getSuggestions(query, page, pageSize);
            }
        }
    }
}
