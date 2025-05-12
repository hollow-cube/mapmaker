package net.hollowcube.mapmaker.gui.map.details;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Switch;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.DisplayName;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;

public class MapDetailsView extends Panel {

    public MapDetailsView(@NotNull MapData mapData, @NotNull DisplayName authorName) {
        super(9, 10);

        background("map_details/container", -10, -32);
        add(0, 0, new Text("", 9, 0, mapData.name())
                .align(30, -23));
        var lowerVariant = mapData.settings().getVariant().toString().toLowerCase(Locale.ROOT);
        add(0, 0, new Button("", 0, 0)
                .sprite("map_details/variant_" + lowerVariant, -5, -27));

        add(0, 0, backOrClose());
        var authorUsername = Objects.requireNonNullElse(authorName.getUsername(), "Unknown");
        add(1, 0, new Text("", 7, 1, "by " + authorUsername)
                .align(Text.CENTER, Text.CENTER)
                .background("generic2/btn/default/7_1")
                .translationKey("gui.map_details.creator_profile", authorName.build()));
        add(8, 0, new Button("gui.map_details.map_info_tab.report_map", 1, 1)
                .background("generic2/btn/default/1_1")
                .sprite("map_details/action/report", 6, 3)); // todo handle click

        var tabs = add(0, 2, new Switch(9, 4, List.of(
                new MapDetailsInfoPanel(),
                new MapDetailsTimesPanel(),
                new MapDetailsRatePanel()
        )));
        tabs.select(0);
        add(0, 1, tabs.button(0, 3, 1,
                "gui.map_details.info.tab", "map_details/info/tab"));
        add(3, 1, tabs.button(1, 3, 1,
                "gui.map_details.times.tab", "map_details/times/tab"));
        add(6, 1, tabs.button(2, 3, 1,
                "gui.map_details.rate.tab", "map_details/rate/tab"));

        // todo tabs

        add(0, 6, new Button("gui.map_details.map_info.boost_map", 3, 3)
                .sprite("map_details/action/boost"));
        add(3, 6, new Button("gui.map_details.play_map", 3, 3)
                .sprite("map_details/action/play")); //todo handle click and support leave
        add(6, 6, new Button("gui.map_details.suggest_similar_maps", 3, 3)
                .sprite("map_details/action/similar"));
    }

}
