package net.hollowcube.mapmaker.gui.map.details;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.panels.*;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.component.HeadProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.text;

class MapDetailsTimesPanel extends Panel {
    private static final String MISSING_TIME = "--:--:---";
    private static final Component MISSING_PLAYER = text("Not set!");
    private static final HeadProfile MISSING_TEXTURE = new HeadProfile(new PlayerSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGE5OWIwNWI5YTFkYjRkMjliNWU2NzNkNzdhZTU0YTc3ZWFiNjY4MTg1ODYwMzVjOGEyMDA1YWViODEwNjAyYSJ9fX0=", null));
    private static final String MODEL_8X = "mapmaker:2d_player_head";
    private static final String MODEL_8X_OFFSET_1 = "mapmaker:2d_player_head_offset1";
    private static final String MODEL_8X_OFFSET_2 = "mapmaker:2d_player_head_offset2";

    private final PlayerService playerService;
    private final MapService mapService;
    private final String mapId;

    private final Switch tabs;
    private final TopThreePanel topThreePanel;
    private final TopTenPanel topTenPanel;

    private final Button playerHeadBtn;
    private final Text playerTimeText;
    private final List<Button> playerButtons; // They need the same text :|

    public MapDetailsTimesPanel(@NotNull PlayerService playerService, @NotNull MapService mapService, @NotNull String mapId) {
        super(9, 4);
        this.playerService = playerService;
        this.mapService = mapService;
        this.mapId = mapId;

        background("map_details/times/footer", 0, 54);

        this.tabs = add(0, 0, new Switch(9, 3, List.of(
                this.topThreePanel = new TopThreePanel(),
                this.topTenPanel = new TopTenPanel()
        )));
        add(1, 3, tabs.toggleButton(1, 1,
                "gui.map_details.top_times_tab.other_top_times",
                "map_details/times/other_times", 2, 1));

        this.playerHeadBtn = add(2, 3, new Button(null, 1, 1));
        this.playerTimeText = add(3, 3, new Text("", 3, 1, MISSING_TIME)
                .align(Text.CENTER, Text.CENTER));
        var playerTimeBtn = add(6, 3, new Button(null, 1, 1));
        this.playerButtons = List.of(playerHeadBtn, playerTimeText, playerTimeBtn);

        add(7, 3, new Button("gui.map_details.top_times_tab.toggle_first_completions", 1, 1));
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        this.tabs.select(0); // Always go back to the top 3 tab

        if (!isInitial) return;

        async(() -> {
            var playerId = PlayerData.fromPlayer(host.player()).id();
            var leaderboard = mapService.getPlaytimeLeaderboard(mapId, playerId);
            var displayNames = playerService.getPlayerDisplayNames(leaderboard.top()
                    .stream().map(LeaderboardData.Entry::player).toList());

            sync(() -> {
                this.topThreePanel.update(leaderboard.top(), displayNames);
                this.topTenPanel.update(leaderboard.top(), displayNames);

                {   // Update the player entry
                    var time = (leaderboard.player() == null ? MISSING_TIME
                            : NumberUtil.formatMapPlaytime(leaderboard.player().score(), true));
                    for (var btn : playerButtons)
                        btn.translationKey("gui.map_details.top_times_tab.personal_best", text(time));
                    playerHeadBtn.profile(getPlayerHead2d(playerId));
                    playerHeadBtn.model(MODEL_8X, null);
                    playerTimeText.text(time);
                }
            });
        });
    }


    private static final Cache<String, HeadProfile> HEAD_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    static @NotNull HeadProfile getPlayerHead2d(@Nullable String uuid) {
        if (uuid == null) return MISSING_TEXTURE;
        return HEAD_CACHE.get(uuid, key -> {
            var profile = OpUtils.map(PlayerSkin.fromUuid(key), HeadProfile::new);
            return Objects.requireNonNullElse(profile, HeadProfile.EMPTY);
        });
    }

    private static class TopThreePanel extends Panel {
        private final Entry[] entries = new Entry[3];

        public TopThreePanel() {
            super(9, 3);
            background("map_details/times/top_three");

            this.entries[1] = add(0, 0, new Entry("second"));
            this.entries[0] = add(3, 0, new Entry("first"));
            this.entries[2] = add(6, 0, new Entry("third"));
        }

        public void update(@NotNull List<LeaderboardData.Entry> entries, @NotNull List<DisplayName> displayNames) {
            for (int i = 0; i < 3; i++) {
                if (i >= entries.size()) continue;

                var entry = entries.get(i);
                var displayName = displayNames.get(i).build(DisplayName.Context.DEFAULT);
                this.entries[i].update(entry, displayName);
            }
        }

        private static class Entry extends Panel {
            private final String translationKey;

            private final Button backgroundBtn;
            private final Button playerHeadBtn;
            private final Text timeText;

            public Entry(@NotNull String number) {
                super(3, 3);
                this.translationKey = "gui.map_details.top_times_tab." + number + "_place";

                // This abuses a bit of a hack that we set the entire area to a button then overlay the model and text
                this.backgroundBtn = add(0, 0, new Button(null, 3, 3)
                        .translationKey(translationKey, MISSING_PLAYER, MISSING_TIME));
                this.playerHeadBtn = add(1, 1, new Button(null, 1, 1)
                        .translationKey(translationKey, MISSING_PLAYER, MISSING_TIME)
                        .model(MODEL_8X, null)
                        .profile(MISSING_TEXTURE));
                this.timeText = add(0, 2, new Text(null, 3, 1, MISSING_TIME)
                        .align(Text.CENTER, 6));
            }

            public void update(@NotNull LeaderboardData.Entry entry, @NotNull Component playerName) {
                var time = NumberUtil.formatMapPlaytime(entry.score(), true);
                backgroundBtn.translationKey(translationKey, playerName, time);
                playerHeadBtn.translationKey(translationKey, playerName, time);
                playerHeadBtn.profile(getPlayerHead2d(entry.player()));
                timeText.text(time);
            }
        }
    }

    private static class TopTenPanel extends Panel {
        private static final int START_OFFSET = 3;

        private final TopTenPanel.Entry[] entries = new TopTenPanel.Entry[6];

        public TopTenPanel() {
            super(9, 3);
            background("map_details/times/top_ten");

            this.entries[0] = add(0, 0, new TopTenPanel.Entry("fourth", false));
            this.entries[1] = add(5, 0, new TopTenPanel.Entry("fifth", true));
            this.entries[2] = add(0, 1, new TopTenPanel.Entry("sixth", false));
            this.entries[3] = add(5, 1, new TopTenPanel.Entry("seventh", true));
            this.entries[4] = add(0, 2, new TopTenPanel.Entry("eighth", false));
            this.entries[5] = add(5, 2, new TopTenPanel.Entry("ninth", true));
        }

        public void update(@NotNull List<LeaderboardData.Entry> entries, @NotNull List<DisplayName> displayNames) {
            for (int i = 0; i < this.entries.length; i++) {
                int index = i + START_OFFSET;
                if (index >= entries.size()) return;

                var entry = entries.get(index);
                var displayName = displayNames.get(index).build(DisplayName.Context.DEFAULT);
                this.entries[i].update(entry, displayName);
            }
        }

        private static class Entry extends Panel {
            private final String translationKey;

            private final Button playerHeadBtn;
            private final Text timeText;

            public Entry(@NotNull String number, boolean isRightColumn) {
                super(4, 1);
                this.translationKey = "gui.map_details.top_times_tab." + number + "_place";

                this.playerHeadBtn = add(0, 0, new Button(null, 1, 1)
                        .translationKey(translationKey, MISSING_PLAYER, MISSING_TIME)
                        .model(isRightColumn ? MODEL_8X_OFFSET_2 : MODEL_8X_OFFSET_1, null)
                        .profile(MISSING_TEXTURE));
                this.timeText = add(1, 0, new Text(null, 3, 1, MISSING_TIME)
                        .align(isRightColumn ? -1 : 5, 5));
                this.timeText.translationKey(translationKey, MISSING_PLAYER, MISSING_TIME);
            }

            public void update(@NotNull LeaderboardData.Entry entry, @NotNull Component playerName) {
                var time = NumberUtil.formatMapPlaytime(entry.score(), true);
                playerHeadBtn.translationKey(translationKey, playerName, time);
                playerHeadBtn.profile(getPlayerHead2d(entry.player()));
                timeText.translationKey(translationKey, playerName, time);
                timeText.text(time);
            }
        }
    }
}
