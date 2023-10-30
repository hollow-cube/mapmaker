package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.gui.play.MapDetailsView;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.player.DisplayName;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapDetailsItem extends ItemHandler {

    public static final String ID = "mapmaker:map_details";
    public static final MapDetailsItem INSTANCE = new MapDetailsItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/map_details"));

    private MapDetailsItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    public int customModelData() {
        return SPRITE.cmd();
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        Thread.startVirtualThread(() -> {
            var player = click.player();
            var server = MapServer.StaticAbuse.instance;

            var map = MapWorld.forPlayer(player).map();
            var authorName = server.playerService().getPlayerDisplayName2(map.owner())
                    .build(DisplayName.Context.PLAIN);
            var personalMapData = new PersonalizedMapData(map, PersonalizedMapData.Progress.NONE);
            server.newOpenGUI(player, c -> new MapDetailsView(c, personalMapData, authorName));
        });
    }

}
