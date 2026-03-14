package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.mapmaker.gui.store.CosmeticPanel;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class OpenCosmeticsMenuItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/cosmetic_menu"));
    public static final Key ID = Key.key("mapmaker:cosmetics");

    private final PlayerService players;

    public OpenCosmeticsMenuItem(PlayerService players) {
        super(ID, RIGHT_CLICK_ANY);
        this.players = players;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        Panel.open(click.player(), new CosmeticPanel(this.players));
    }

}
