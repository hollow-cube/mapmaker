package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.mapmaker.gui.store.StoreView;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class OpenStoreItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/store_menu"));

    public static final Key ID = Key.key("mapmaker:store");
    public static final OpenStoreItem INSTANCE = new OpenStoreItem();

    private OpenStoreItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var world = MapWorld.forPlayer(click.player());
        if (world == null) return; // Sanity

        Panel.open(click.player(), new StoreView(world.server().playerService()));
    }

}
