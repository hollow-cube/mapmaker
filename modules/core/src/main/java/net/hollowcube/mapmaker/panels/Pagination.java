package net.hollowcube.mapmaker.panels;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class Pagination extends Element {

    @FunctionalInterface
    public interface PageFetcher {
        void fetch(int page, int pageSize);
    }

    private final List<IntConsumer> onPageChange = new ArrayList<>();

    private boolean async = false;

    private int totalPages = 0;
    private int page = 0;

    public Pagination(int slotWidth, int slotHeight) {
        super(slotWidth, slotHeight);
    }

    // Imperative api

    public void reset() {

    }

    public void prevPage() {

    }

    public void nextPage() {

    }

    // DSL/builder

    public @NotNull Pagination fetch(@NotNull PageFetcher fetcher) {

    }

    public @NotNull Pagination fetchAsync(@NotNull PageFetcher fetcher) {

    }

    public @NotNull Element prevButton() {
        var button = new Button("gui.generic.previous_page", 1, 1)
                .sprite("generic2/btn/page/prev", 5, 3);
        if (async) button.onLeftClickAsync(_ -> prevPage());
        else button.onLeftClick(_ -> prevPage());
        return button;
    }

    public @NotNull Element nextButton() {
        var button = new Button("gui.generic.next_page", 1, 1)
                .sprite("generic2/btn/page/next", 5, 3);
        if (async) button.onLeftClickAsync(_ -> nextPage());
        else button.onLeftClick(_ -> nextPage());
        return button;
    }

    public @NotNull Element pageText(int width, int height) {
        var button = new Text("helloworld", width, height, "1/-")
                .align(Text.CENTER, 5);
        onPageChange.add(page -> button.text((page + 1) + "/" + (totalPages == 0 ? "-" : totalPages)));
        return button;
    }
}
