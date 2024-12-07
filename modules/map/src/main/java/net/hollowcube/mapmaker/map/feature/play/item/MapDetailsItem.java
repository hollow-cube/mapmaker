package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.gui.play.MapDetailsView;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.MinecraftServer;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MapDetailsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/map_details"));
    public static final String ID = "mapmaker:map_details";
    public static final MapDetailsItem INSTANCE = new MapDetailsItem();

    private MapDetailsItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        FutureUtil.submitVirtual(() -> {
            DisplayName authorName;
            try {
                authorName = world.server().playerService().getPlayerDisplayName2(world.map().owner());
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
                authorName = new DisplayName(List.of(new DisplayName.Part("username", "!error!", null)));
            }
            DisplayName finalAuthorName = authorName;
            world.server().showView(player, c -> new MapDetailsView(c, world.map(), finalAuthorName));
        });
    }

}
