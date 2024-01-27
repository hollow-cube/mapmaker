package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.util.GenericTempActionBarProvider;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SetSpectatorCheckpointItem extends ItemHandler {

    public static Tag<Pos> SPECTATOR_CHECKPOINT = Tag.Transient("mapmaker:spectator_checkpoint");

    public static final String ID_SPECTATOR = "mapmaker:set_spectator_checkpoint";
    public static final SetSpectatorCheckpointItem INSTANCE_SPECTATOR = new SetSpectatorCheckpointItem(ID_SPECTATOR);
    public static final String ID_TESTING = "mapmaker:set_testing_checkpoint";
    public static final SetSpectatorCheckpointItem INSTANCE_TESTING = new SetSpectatorCheckpointItem(ID_TESTING);

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/set_checkpoint"));

    private SetSpectatorCheckpointItem(@NotNull String id) {
        super(id, RIGHT_CLICK_ANY);
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
        if (player.isSneaking()) {
            player.removeTag(SPECTATOR_CHECKPOINT);
            sendActionBar(player, "Cleared temporary checkpoint!");
        } else {
            player.setTag(SPECTATOR_CHECKPOINT, player.getPosition());
            sendActionBar(player, "Added temporary checkpoint!");
        }
    }

    private void sendActionBar(@NotNull Player player, @NotNull String message) {
        var ab = ActionBar.forPlayer(player);
        ab.addProvider(new GenericTempActionBarProvider(message, 1000L));
    }

}
