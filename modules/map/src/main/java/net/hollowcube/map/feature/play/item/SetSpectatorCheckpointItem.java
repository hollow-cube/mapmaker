package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SetSpectatorCheckpointItem extends ItemHandler {

    public static Tag<Pos> SPECTATOR_CHECKPOINT = Tag.Transient("mapmaker:spectator_checkpoint");

    public static final String ID = "mapmaker:set_spectator_checkpoint";
    public static final SetSpectatorCheckpointItem INSTANCE = new SetSpectatorCheckpointItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/set_checkpoint"));

    private SetSpectatorCheckpointItem() {
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
        var player = click.player();
        player.setTag(SPECTATOR_CHECKPOINT, player.getPosition());
        System.out.println("player spectator checkpoint is: " + player.getTag(SPECTATOR_CHECKPOINT).toString());
    }
}
