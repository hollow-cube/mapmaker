package net.hollowcube.mapmaker.gui.play.details;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.metadata.PlayerHeadMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DetailsTimesTabView extends View {

    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private @Outlet("map_author") Text mapAuthorText;

    private @Outlet("view_switch") Switch viewSwitch;
    private @Outlet("top_three") TimesTopThreeView topThreeView;
    private @Outlet("top_ten") TimesTopTenView topTenView;
    private @Outlet("viewer_entry") Text viewerEntry;

    private MapData map = null;
    private LeaderboardData leaderboard = null;
    private Future<?> loadFuture;

    public DetailsTimesTabView(@NotNull Context context) {
        super(context);

        viewSwitch.setState(State.LOADING);
    }

    // Required immediately on load, but no requests will be made until a #show call is made.
    public void setMap(@NotNull MapData map, @NotNull String authorTextContent, @NotNull Component authorDisplayName) {
        this.map = map;

        mapAuthorText.setText(authorTextContent);
        mapAuthorText.setArgs(authorDisplayName);
    }

    public void show() {
        if (loadFuture != null || leaderboard != null) return;
        loadFuture = async(() -> {
            try {
                var mapId = Objects.requireNonNull(map, "times tab view was not initialized").id();
                var playerId = PlayerDataV2.fromPlayer(player).id();
                var leaderboard = mapService.getPlaytimeLeaderboard(mapId, playerId);

                topThreeView.fillEntries(leaderboard.top());
                topTenView.fillEntries(leaderboard.top());
                fillPlayerEntry(leaderboard.player());
                this.leaderboard = leaderboard;
                viewSwitch.setState(State.ACTIVE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                loadFuture = null;
            }
        });
    }

    @Action("toggle_top_ten_btn")
    private void handleToggleView() {
        viewSwitch.setOption(viewSwitch.getOption() == 0 ? 1 : 0);
    }

    private void fillPlayerEntry(@Nullable LeaderboardData.Entry entry) {
        var playerId = PlayerDataV2.fromPlayer(player).id();

        viewerEntry.setItemSprite(getPlayerHead2d(playerId, MODEL_8X));
        viewerEntry.setText(FontUtil.computeOffset(2) + (entry == null ? MISSING_TIME : NumberUtil.formatMapPlaytime(entry.score(), true)));
    }

    static final String MISSING_TIME = "--:--:---";
    static final ItemStack MISSING_ITEM = ItemStack.builder(Material.PLAYER_HEAD)
            .meta(PlayerHeadMeta.class, meta -> {
                meta.playerSkin(new PlayerSkin("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGE5OWIwNWI5YTFkYjRkMjliNWU2NzNkNzdhZTU0YTc3ZWFiNjY4MTg1ODYwMzVjOGEyMDA1YWViODEwNjAyYSJ9fX0=", null));
            })
            .build();
    static final int MODEL_8X = 1;
    static final int MODEL_8X_OFFSET_1 = 3;
    static final int MODEL_8X_OFFSET_2 = 4;
    private static final Cache<String, ItemStack> HEAD_CACHE = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    static @NotNull ItemStack getPlayerHead2d(@Nullable String uuid, int model) {
        if (uuid == null) return MISSING_ITEM.withMeta(meta -> meta.customModelData(model));
        return HEAD_CACHE.get(uuid, key -> {
            var skin = PlayerSkin.fromUuid(key);
            return ItemStack.builder(Material.PLAYER_HEAD)
                    .meta(PlayerHeadMeta.class, meta -> {
                        meta.skullOwner(UUID.fromString(key));
                        meta.playerSkin(skin);
                    })
                    .build();
        }).withMeta(meta -> meta.customModelData(model));
    }

}
