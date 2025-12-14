package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ResetSaveStateItem extends ItemHandler {

    private static final TagCooldown CONFIRM_COOLDOWN = new TagCooldown("mapmaker:play/reset_item_confirm", 2000);

    private static final int MIN_RESET_ITEM_TIME = 60 * 1000; // 1 minute
    private static final int MIN_RESET_SCREEN_TIME = 60 * 5000; // 5 minutes

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/reset"));
    public static final Key ID = Key.key("mapmaker:reset_savestate");
    public static final ResetSaveStateItem INSTANCE = new ResetSaveStateItem();

    private ResetSaveStateItem() {
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

        var currentState = world.getPlayerState(player);
        if (currentState == null) return;
        if (currentState instanceof ParkourState.AnyPlaying parkourState) {
            var saveState = parkourState.saveState();
            var playtime = saveState.getRealPlaytime();
            if (playtime > MIN_RESET_SCREEN_TIME) {
                Panel.open(player, ExtraPanels.confirm("Reset Map Progress?", () -> world.hardResetPlayer(player)));
                return;
            } else if ((playtime > MIN_RESET_ITEM_TIME || saveState.state(PlayState.class).lastState() != null) && CONFIRM_COOLDOWN.test(player)) {
                player.sendMessage(Component.translatable("item.mapmaker.reset_savestate.confirm"));
                return;
            }
        }

        world.hardResetPlayer(player);
    }

}
