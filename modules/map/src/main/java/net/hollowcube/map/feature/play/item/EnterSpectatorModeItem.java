package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class EnterSpectatorModeItem extends ItemHandler {

    public static final String ID = "mapmaker:enter_spectator";
    public static final EnterSpectatorModeItem INSTANCE = new EnterSpectatorModeItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/enter_spectator"));

    private EnterSpectatorModeItem() {
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
        var world = (InternalMapWorld) MapWorld.forPlayer(player);

        if (player.isOnGround()) {
            if (world instanceof PlayingMapWorld playingWorld) {
                world.removePlayer(player);
                player.setTag(SPECTATOR_CHECKPOINT, player.getPosition());
                playingWorld.startSpectating(player, false);
            }
        } else {
            player.sendMessage(Component.translatable("map.spectator_mode.solid_ground"));
        }
    }

}
