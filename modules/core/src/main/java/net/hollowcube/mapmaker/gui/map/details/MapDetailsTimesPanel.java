package net.hollowcube.mapmaker.gui.map.details;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.map.Leaderboard;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.util.CoreSkulls;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.network.player.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

public class MapDetailsTimesPanel extends Panel {
    private static final String MISSING_SCORE = "-";
    private static final Component MISSING_PLAYER = text("Not set!");
    public static final String MODEL_8X = "mapmaker:2d_player_head";
    private static final String MODEL_8X_OFFSET_1 = "mapmaker:2d_player_head_offset1";
    private static final String MODEL_8X_OFFSET_2 = "mapmaker:2d_player_head_offset2";

    private final ApiClient api;
    private final MapService mapService;
    private final MapData map;
    private final Leaderboard.Format lbFormat;

    private final Switch tabs;
    private final TopThreePanel topThreePanel;
    private final TopTenPanel topTenPanel;

    private final Button playerHeadBtn;
    private final Text playerTimeText;
    private final List<Button> playerButtons; // They need the same text :|

    public MapDetailsTimesPanel(ApiClient api, MapService mapService, MapData map) {
        super(9, 4);
        this.api = api;
        this.mapService = mapService;
        this.map = map;
        this.lbFormat = map.settings().leaderboard().format();

        background("map_details/times/footer", 0, 54);

        this.tabs = add(0, 0, new Switch(9, 3, List.of(
            this.topThreePanel = new TopThreePanel(lbFormat),
            this.topTenPanel = new TopTenPanel(lbFormat)
        )));
        add(1, 3, tabs.toggleButton(1, 1,
            "gui.map_details.top_times_tab.other_top_times",
            "map_details/times/other_times", 2, 1));

        this.playerHeadBtn = add(2, 3, new Button(null, 1, 1));
        this.playerTimeText = add(3, 3, new Text("", 3, 1, MISSING_SCORE)
            .align(Text.CENTER, Text.CENTER));
        var playerTimeBtn = add(6, 3, new Button(null, 1, 1));
        this.playerButtons = List.of(playerHeadBtn, playerTimeText, playerTimeBtn);

        add(7, 3, new Button("gui.map_details.top_times_tab.toggle_first_completions", 1, 1));
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        this.tabs.select(0); // Always go back to the top 3 tab

        if (!isInitial) return;

        async(() -> {
            var playerId = PlayerData.fromPlayer(host.player()).id();
            var leaderboard = mapService.getPlaytimeLeaderboard(map.id(), playerId);
            // TODO: bulk endpoint?
            var displayNames = leaderboard.top().stream()
                .map(LeaderboardData.Entry::player)
                .map(api.players::getDisplayName)
                .toList();

            sync(() -> {
                this.topThreePanel.update(leaderboard.top(), displayNames);
                this.topTenPanel.update(leaderboard.top(), displayNames);

                {   // Update the player entry
                    var time = (leaderboard.player() == null ? MISSING_SCORE
                        : lbFormat.formatPlain(leaderboard.player().score()));
                    for (var btn : playerButtons)
                        btn.translationKey("gui.map_details.top_times_tab.personal_best", text(time));
                    playerHeadBtn.profile(getPlayerHead2d(playerId));
                    playerHeadBtn.model(MODEL_8X, null);
                    playerTimeText.text(time);
                }
            });
        });
    }


    private static final Cache<String, ResolvableProfile> HEAD_CACHE = Caffeine.newBuilder()
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build();

    public static ResolvableProfile getPlayerHead2d(@Nullable String uuid) {
        if (uuid == null) return CoreSkulls.UNKNOWN_PLAYER;
        return HEAD_CACHE.get(uuid, key -> OpUtils.mapOr(
            PlayerSkin.fromUuid(key),
            CoreSkulls::create,
            ResolvableProfile.EMPTY
        ));
    }

    private static class TopThreePanel extends Panel {
        private final Entry[] entries = new Entry[3];

        public TopThreePanel(Leaderboard.Format lbFormat) {
            super(9, 3);
            background("map_details/times/top_three");

            this.entries[1] = add(0, 0, new Entry(lbFormat, "second"));
            this.entries[0] = add(3, 0, new Entry(lbFormat, "first"));
            this.entries[2] = add(6, 0, new Entry(lbFormat, "third"));
        }

        public void update(List<LeaderboardData.Entry> entries, List<DisplayName> displayNames) {
            for (int i = 0; i < 3; i++) {
                if (i >= entries.size()) continue;

                var entry = entries.get(i);
                var displayName = displayNames.get(i).build(DisplayName.Context.DEFAULT);
                this.entries[i].update(entry, displayName);
            }
        }

        private static class Entry extends Panel {
            private final Leaderboard.Format lbFormat;
            private final String translationKey;

            private final Button backgroundBtn;
            private final Button playerHeadBtn;
            private final Text scoreText;

            public Entry(Leaderboard.Format lbFormat, String number) {
                super(3, 3);
                this.lbFormat = lbFormat;
                this.translationKey = "gui.map_details.top_times_tab." + number + "_place";

                // This abuses a bit of a hack that we set the entire area to a button then overlay the model and text
                this.backgroundBtn = add(0, 0, new Button(null, 3, 3)
                    .translationKey(translationKey, MISSING_PLAYER, MISSING_SCORE));
                this.playerHeadBtn = add(1, 1, new Button(null, 1, 1)
                    .translationKey(translationKey, MISSING_PLAYER, MISSING_SCORE)
                    .model(MODEL_8X, null)
                    .profile(CoreSkulls.UNKNOWN_PLAYER));
                this.scoreText = add(0, 2, new Text(null, 3, 1, MISSING_SCORE)
                    .align(Text.CENTER, 6));
            }

            public void update(LeaderboardData.Entry entry, Component playerName) {
                var time = lbFormat.formatPlain(entry.score());
                backgroundBtn.translationKey(translationKey, playerName, time);
                playerHeadBtn.translationKey(translationKey, playerName, time);
                playerHeadBtn.profile(getPlayerHead2d(entry.player()));
                scoreText.text(time);
            }
        }
    }

    private static class TopTenPanel extends Panel {
        private static final int START_OFFSET = 3;

        private final TopTenPanel.Entry[] entries = new TopTenPanel.Entry[6];

        public TopTenPanel(Leaderboard.Format lbFormat) {
            super(9, 3);

            background("map_details/times/top_ten");

            this.entries[0] = add(0, 0, new TopTenPanel.Entry(lbFormat, "fourth", false));
            this.entries[1] = add(5, 0, new TopTenPanel.Entry(lbFormat, "fifth", true));
            this.entries[2] = add(0, 1, new TopTenPanel.Entry(lbFormat, "sixth", false));
            this.entries[3] = add(5, 1, new TopTenPanel.Entry(lbFormat, "seventh", true));
            this.entries[4] = add(0, 2, new TopTenPanel.Entry(lbFormat, "eighth", false));
            this.entries[5] = add(5, 2, new TopTenPanel.Entry(lbFormat, "ninth", true));
        }

        public void update(List<LeaderboardData.Entry> entries, List<DisplayName> displayNames) {
            for (int i = 0; i < this.entries.length; i++) {
                int index = i + START_OFFSET;
                if (index >= entries.size()) return;

                var entry = entries.get(index);
                var displayName = displayNames.get(index).build(DisplayName.Context.DEFAULT);
                this.entries[i].update(entry, displayName);
            }
        }

        private static class Entry extends Panel {
            private final Leaderboard.Format lbFormat;
            private final String translationKey;

            private final Button playerHeadBtn;
            private final Text timeText;

            public Entry(Leaderboard.Format lbFormat, String number, boolean isRightColumn) {
                super(4, 1);
                this.lbFormat = lbFormat;
                this.translationKey = "gui.map_details.top_times_tab." + number + "_place";

                this.playerHeadBtn = add(0, 0, new Button(null, 1, 1)
                    .translationKey(translationKey, MISSING_PLAYER, MISSING_SCORE)
                    .model(isRightColumn ? MODEL_8X_OFFSET_2 : MODEL_8X_OFFSET_1, null)
                    .profile(CoreSkulls.UNKNOWN_PLAYER));
                this.timeText = add(1, 0, new Text(null, 3, 1, MISSING_SCORE)
                    .align(isRightColumn ? -1 : 5, 5));
                this.timeText.translationKey(translationKey, MISSING_PLAYER, MISSING_SCORE);
            }

            public void update(LeaderboardData.Entry entry, Component playerName) {
                var time = lbFormat.formatPlain(entry.score());
                playerHeadBtn.translationKey(translationKey, playerName, time);
                playerHeadBtn.profile(getPlayerHead2d(entry.player()));
                timeText.translationKey(translationKey, playerName, time);
                timeText.text(time);
            }
        }
    }
}
