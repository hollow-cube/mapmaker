package net.hollowcube.mapmaker.gui.map.details;

import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Switch;

import java.util.List;

class MapDetailsTimesPanel extends Panel {
    public MapDetailsTimesPanel() {
        super(9, 4);

        var tabs = add(0, 0, new Switch(9, 3, List.of(
                new TopThreePanel(),
                new TopTenPanel()
        )));
        add(1, 3, tabs.toggleButton(1, 1,
                "gui.map_details.top_times_tab.other_top_times",
                "map_details/times/other_times"));
    }

    private static class TopThreePanel extends Panel {
        public TopThreePanel() {
            super(9, 3);
            background("map_details/times/top_three");
        }
    }

    private static class TopTenPanel extends Panel {
        public TopTenPanel() {
            super(9, 3);
            background("map_details/times/top_ten");
        }
    }
}
