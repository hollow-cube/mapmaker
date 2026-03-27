package net.hollowcube.mapmaker.gui.map.browser;

import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.requests.MapSearchParams;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Element;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class MapContestBrowserView extends MapBrowserView {

    public MapContestBrowserView(@NotNull ApiClient api, @NotNull MapService mapService, @NotNull ServerBridge bridge) {
        this(api, mapService, bridge, true);
    }

    public MapContestBrowserView(
        @NotNull ApiClient api, @NotNull MapService mapService, @NotNull ServerBridge bridge,
        boolean fetchOnMount
    ) {
        super(api, mapService, bridge, fetchOnMount);

        background("map_browser/contest_container", -10, -31);
        this.titleText.text("Comp Submissions");

        ignoreParamsOnSearch = false;
    }

    @Override
    protected Element createBottomPanel() {
        return new BottomPanel();
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        if (!isInitial) return;

        var playerId = host.player().getUuid().toString();
        pagination.reset(MapSearchParams.builder(playerId)
            .contest("c9354e33-96c2-414a-9f4a-8c2ff4669086"));
    }

    private static class BottomPanel extends Panel {

        public BottomPanel() {
            super(9, 3);

            add(2, 2, new Button("gui.competition.items.submissions.info_hover", 5, 1)
                .onLeftClick(() -> {
                    host.player().sendMessage(Component.translatable("chat.competition.link"));
                    host.player().closeInventory();
                }));
        }
    }
}
