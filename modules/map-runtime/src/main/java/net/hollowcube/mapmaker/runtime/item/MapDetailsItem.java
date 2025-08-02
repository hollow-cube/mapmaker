package net.hollowcube.mapmaker.runtime.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.map.details.MapDetailsView;
import net.hollowcube.mapmaker.map.MapWorld2;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MapDetailsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/map_details"));
    public static final Key ID = Key.key("mapmaker:map_details");
    public static final MapDetailsItem INSTANCE = new MapDetailsItem();

    private MapDetailsItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = MapWorld2.forPlayer(player);
        if (world == null) return;

        FutureUtil.submitVirtual(() -> {
            DisplayName authorName;
            try {
                authorName = world.server().playerService().getPlayerDisplayName2(world.map().owner());
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
                authorName = new DisplayName(List.of(new DisplayName.Part("username", "!error!", null)));
            }
            DisplayName finalAuthorName = authorName;

            Panel.open(player, new MapDetailsView(world.server().playerService(), world.server().mapService(),
                    world.server().bridge(), world.map(), finalAuthorName, false));
        });
    }

}
