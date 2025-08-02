package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld2;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ReturnToCheckpointItem extends ItemHandler {
    private static final TagCooldown CONFIRM_COOLDOWN = new TagCooldown("mapmaker:play/checkpoint_item_confirm", 2000);
    private static final int MIN_RESET_TIME = 60 * 1000; // 1 minute

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
        var world = ParkourMapWorld2.forPlayer(player);
        if (world == null) return;

        if (!(world.getPlayerState(player) instanceof ParkourState.Playing(var saveState, var isScorable)))
            return; // Don't do anything when not playing, not that you should have the item anyway.

        boolean confirm = isScorable // Never confirm for non-scorable runs
                // Must have been playing for longer than the minimum reset time
                && saveState.getRealPlaytime() > MIN_RESET_TIME
                // Must not have hit a checkpoint yet.
                && saveState.state(PlayState.class).lastState() == null;
        if (confirm && CONFIRM_COOLDOWN.test(player)) {
            // This only happens without a checkpoint, so it acts as a full reset anyway
            // so its fine to use the same message as a full reset.
            player.sendMessage(Component.translatable("item.mapmaker.reset_savestate.confirm"));
            return;
        }

        world.softResetPlayer(player);
    }

}
