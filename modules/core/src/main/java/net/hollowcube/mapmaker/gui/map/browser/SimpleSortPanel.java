package net.hollowcube.mapmaker.gui.map.browser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapQuality;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Switch;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerSetting;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

class SimpleSortPanel extends Panel {
    private static final List<MapData.Difficulty> DEFAULT_DIFFICULTIES = List.of(MapData.Difficulty.EASY, MapData.Difficulty.MEDIUM, MapData.Difficulty.HARD);

    private static final PlayerSetting<MapBrowserView.SortPreset> SORT_PRESET = PlayerSetting.Enum("map_browser.sort_preset", MapBrowserView.SortPreset.BEST);
    private static final PlayerSetting<List<MapData.Difficulty>> DIFFICULTIES = PlayerSetting.create("map_browser.difficulties",
            DEFAULT_DIFFICULTIES, SimpleSortPanel::writeDifficultyList, SimpleSortPanel::readDifficultyList);

    private final Consumer<MapSearchParams.Builder> onSearch;
    private final boolean fetchOnMount;

    private final Set<MapData.Difficulty> difficulties = new HashSet<>();
    private MapBrowserView.SortPreset sort = MapBrowserView.SortPreset.BEST;

    private final Switch sortSwitch;
    private final DifficultyToggleButton easy, medium, hard, expert, nightmare;

    private boolean sync = true;

    public SimpleSortPanel(@NotNull Consumer<MapSearchParams.Builder> onSearch, boolean fetchOnMount) {
        super(9, 4);
        this.onSearch = onSearch;
        this.fetchOnMount = fetchOnMount;

        // Tabs (we abuse a switch that never gets added to the panel for its single select buttons)
        sortSwitch = new Switch(0, 0, List.of(Panel.EMPTY, Panel.EMPTY, Panel.EMPTY));
        add(0, 0, sortSwitch.button(0, 3, 1, "gui.map_browser.sort_best", "map_browser/tabs/best"));
        add(3, 0, sortSwitch.button(1, 3, 1, "gui.map_browser.sort_quality", "map_browser/tabs/quality"));
        add(6, 0, sortSwitch.button(2, 3, 1, "gui.map_browser.sort_new", "map_browser/tabs/new"));
        sortSwitch.onSelect(index -> selectSort(MapBrowserView.SortPreset.values()[index]));

        // Difficulty
        easy = add(2, 2, new DifficultyToggleButton("gui.map_browser.difficulty_easy",
                "map_browser/difficulty/easy", 4, 3)
                .onChange(selected -> selectDifficulty(MapData.Difficulty.EASY, selected)));
        medium = add(3, 2, new DifficultyToggleButton("gui.map_browser.difficulty_medium",
                "map_browser/difficulty/medium", 3, 3)
                .onChange(selected -> selectDifficulty(MapData.Difficulty.MEDIUM, selected)));
        hard = add(4, 2, new DifficultyToggleButton("gui.map_browser.difficulty_hard",
                "map_browser/difficulty/hard", 4, 4)
                .onChange(selected -> selectDifficulty(MapData.Difficulty.HARD, selected)));
        expert = add(5, 2, new DifficultyToggleButton("gui.map_browser.difficulty_expert",
                "map_browser/difficulty/expert", 4, 4)
                .onChange(selected -> selectDifficulty(MapData.Difficulty.EXPERT, selected)));
        nightmare = add(6, 2, new DifficultyToggleButton("gui.map_browser.difficulty_nightmare",
                "map_browser/difficulty/nightmare", 4, 4)
                .onChange(selected -> selectDifficulty(MapData.Difficulty.NIGHTMARE, selected)));

        // Complex sort placeholder
        add(1, 3, new Button("gui.map_browser.advanced_search", 7, 1));
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        var playerData = PlayerDataV2.fromPlayer(host.player());
        this.sort = playerData.getSetting(SORT_PRESET);
        sortSwitch.select(sort.ordinal());

        this.difficulties.clear();
        this.difficulties.addAll(playerData.getSetting(DIFFICULTIES));
        easy.setSelected(this.difficulties.contains(MapData.Difficulty.EASY));
        medium.setSelected(this.difficulties.contains(MapData.Difficulty.MEDIUM));
        hard.setSelected(this.difficulties.contains(MapData.Difficulty.HARD));
        expert.setSelected(this.difficulties.contains(MapData.Difficulty.EXPERT));
        nightmare.setSelected(this.difficulties.contains(MapData.Difficulty.NIGHTMARE));

        super.mount(host, isInitial);

        if (isInitial && fetchOnMount) onSearchChange();
    }

    public void setSync(boolean sync) {
        this.sync = sync;
    }

    public void setSort(@NotNull MapBrowserView.SortPreset sort) {
        if (this.sort == sort) onSearchChange();
        else this.sortSwitch.select(sort.ordinal());
    }

    private void selectSort(@NotNull MapBrowserView.SortPreset sort) {
        if (this.sort == sort) return;
        this.sort = sort;
        onSearchChange();

        if (host == null || !sync) return;
        var playerData = PlayerDataV2.fromPlayer(host.player());
        playerData.setSetting(SORT_PRESET, sort);
    }

    private void selectDifficulty(@NotNull MapData.Difficulty difficulty, boolean selected) {
        var changed = selected ? difficulties.add(difficulty) : difficulties.remove(difficulty);
        if (!changed) return;
        onSearchChange();

        if (host == null || !sync) return;
        var playerData = PlayerDataV2.fromPlayer(host.player());
        playerData.setSetting(DIFFICULTIES, new ArrayList<>(difficulties));
    }

    private void onSearchChange() {
        var params = MapSearchParams.builder(host.player().getUuid().toString());
        if (!difficulties.isEmpty() && difficulties.size() != 5) {
            // Only set if not 0 or all. In those cases we also want to include unknown so can use default.
            params.difficulties(difficulties.toArray(new MapData.Difficulty[0]));
        }
        switch (sort) {
            case BEST -> params
                    .ascending(false)
                    .best(true)
                    .qualities(MapQuality.GOOD, MapQuality.GREAT, MapQuality.EXCELLENT, MapQuality.OUTSTANDING, MapQuality.MASTERPIECE);
            case QUALITY -> params
                    .ascending(false)
                    .qualities(MapQuality.GREAT, MapQuality.EXCELLENT, MapQuality.OUTSTANDING, MapQuality.MASTERPIECE);
            case NEW -> params
                    .ascending(false)
                    .best(false);
        }
        this.onSearch.accept(params);
    }

    private static List<MapData.Difficulty> readDifficultyList(JsonElement elem) {
        if (!(elem instanceof JsonArray array)) return DEFAULT_DIFFICULTIES;

        var difficulties = new ArrayList<MapData.Difficulty>();
        var values = MapData.Difficulty.values();
        for (var e : array) {
            if (!(e instanceof JsonElement inner)) continue;
            difficulties.add(values[inner.getAsInt()]);
        }
        return difficulties;
    }

    private static JsonArray writeDifficultyList(List<MapData.Difficulty> difficulties) {
        var array = new JsonArray();
        for (var difficulty : difficulties) {
            array.add(difficulty.ordinal());
        }
        return array;
    }
}
