package net.hollowcube.mapmaker.editor.item;

import net.hollowcube.mapmaker.editor.gui.BuilderMenuPanel;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class BuilderMenuItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hammer"));

    public static final Key ID = Key.key("mapmaker:builder_menu");
    public static final BuilderMenuItem INSTANCE = new BuilderMenuItem();

    private BuilderMenuItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = MapWorld.forPlayer(player);
        if (world == null) return; // Sanity

        Panel.open(player, new BuilderMenuPanel(world.server().bridge()));
    }

}
