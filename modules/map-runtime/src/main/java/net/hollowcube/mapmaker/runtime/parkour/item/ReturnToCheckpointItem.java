package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ReturnToCheckpointItem extends ItemHandler {
    private static final TagCooldown CONFIRM_COOLDOWN = new TagCooldown("mapmaker:play/checkpoint_item_confirm", 2000);
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/return_to_checkpoint"));
    public static final Key ID = Key.key("mapmaker:return_to_checkpoint");
    public static final ReturnToCheckpointItem INSTANCE = new ReturnToCheckpointItem();

    private ReturnToCheckpointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        switch (world.getPlayerState(player)) {
            case ParkourState.AnyPlaying p -> {
                boolean needsConfirmation = p.isScorable() && p.saveState().state(PlayState.class).lastState() == null;
                if (needsConfirmation && ResetSaveStateItem.tryConfirmation(player, p, CONFIRM_COOLDOWN, () -> world.softResetPlayer(player))) {
                    return;
                }

                world.softResetPlayer(player);
            }
            case ParkourState.Spectating(var _, var gameState) -> {
                if (gameState.pos() != null) {
                    player.teleport(gameState.pos(), Vec.ZERO, null, RelativeFlags.NONE);
                }
            }
            case null, default -> {
            }
        }
    }

}
