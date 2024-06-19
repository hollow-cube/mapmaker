package net.hollowcube.mapmaker.map.hdb.gui;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.hdb.HeadInfo;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HdbBrowserView extends View {
    private static final String DEFAULT_CATEGORY = "alphabet";

    private static final String ALPHABET_CATEGORY = "alphabet";

    private @ContextObject HeadDatabase hdb;

    private @Outlet("title") Text titleText;
    private @Outlet("heads") Pagination headsPagination;
    private @Outlet("hdb_browser_title") Text hdbBrowserTitle; //TODO make these match the translation keys without creating aids

    private String category;
    private String subCategory; // Only used for alphabet

    public HdbBrowserView(@NotNull Context context) {
        this(context, DEFAULT_CATEGORY);
        titleText.setText("HeadDatabase Browser");
        hdbBrowserTitle.setText(category);
        hdbBrowserTitle.setArgs(Component.text(category));
    }

    public HdbBrowserView(@NotNull Context context, @NotNull String category) {
        super(context);
        this.category = category;
    }

    @Action("heads")
    private void createHeadsList(@NotNull Pagination.PageRequest<HeadIconView> request) {
        var heads = hdb.heads(category);
        if (heads.isEmpty()) {
            request.respond(List.of(new HeadIconView(request.context())), false);
            return;
        }

        // Gross special handling for the alphabet category, but oh well.
        if (ALPHABET_CATEGORY.equals(category)) {
            if (subCategory == null) {
                var names = new HashSet<String>();
                var icons = new ArrayList<HeadInfo>();
                for (var head : heads) {
                    for (var tag : head.tags()) {
                        if (!tag.contains("Font") || names.contains(tag)) continue;
                        names.add(tag);
                        icons.add(new HeadInfo("0", tag, "__alphabet", head.texture(), List.of()));
                        break;
                    }
                }
                heads = icons;
            } else {
                // Otherwise we need to filter the heads
                var newHeads = new ArrayList<HeadInfo>();
                for (var head : heads) {
                    if (!head.tags().contains(subCategory)) continue;
                    newHeads.add(head);
                }
                heads = newHeads;
            }
        }

        var result = new ArrayList<HeadIconView>();
        int offset = request.page() * request.pageSize();
        for (int i = offset; i < offset + request.pageSize() && heads.size() > i; i++) {
            result.add(new HeadIconView(request.context(), heads.get(i)));
        }
        request.respond(result, heads.size() > offset + request.pageSize());
    }

    @Action("prev_page")
    private void handlePrevPage() {
        headsPagination.prevPage();
    }

    @Action("next_page")
    private void handleNextPage() {
        headsPagination.nextPage();
    }

    @Action("categories")
    private void createCategoryList(@NotNull Pagination.PageRequest<CategoryIconView> request) {
        var result = new ArrayList<CategoryIconView>();
        for (var category : hdb.categories())
            result.add(new CategoryIconView(request.context(), hdb, category));
        request.respond(result, false);
    }

    @Action("hdb_browser_search")
    private void openSearchMenu() {
        pushView(context -> new HdbSearchView(context, "")); //TODO make the back button work in this UI
    }

    @Signal(CategoryIconView.SIG_SELECTED)
    private void handleCategoryChanged(@NotNull String newCategory) { //TODO make a selected background sprite for the currently selected category
        if (newCategory.contains("|")) {
            var split = newCategory.split("\\|");
            this.category = split[0];
            this.subCategory = split[1];
        } else {
            this.category = newCategory;
            this.subCategory = null;
        }
        hdbBrowserTitle.setText(category);
        hdbBrowserTitle.setArgs(Component.text(category));
        this.headsPagination.reset();
    }
}
