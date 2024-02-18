package net.hollowcube.mapmaker.map.feature.edit.item;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class BuilderMenuItem extends ItemHandler {

    public static final String ID = "mapmaker:builder_menu";
    public static final BuilderMenuItem INSTANCE = new BuilderMenuItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hammer"));

    private BuilderMenuItem() {
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
        click.player().sendMessage("this will open the builder menu once we have it :)");
    }

}
