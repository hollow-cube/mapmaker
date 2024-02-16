package net.hollowcube.map.feature.play.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.item.handler.ItemHandler;
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
        var world = MapWorld.forPlayerOptional(player);
        if (!(world instanceof PlayingMapWorld)) return;

        // Must be standing on the ground (not falling) to enter spectator mode
        if (!player.isOnGround()) {
            player.sendMessage(Component.translatable("map.spectator_mode.solid_ground"));
            return;
        }

        FutureUtil.submitVirtual(() -> {
            world.removePlayer(player);
            world.addSpectator(player);
            player.setTag(SPECTATOR_CHECKPOINT, player.getPosition());
        });
    }

}
