package net.hollowcube.mapmaker.map.feature.edit.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static net.hollowcube.mapmaker.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class EnterTestModeItem extends ItemHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnterTestModeItem.class);

    public static final String ID = "mapmaker:enter_test_mode";
    public static final EnterTestModeItem INSTANCE = new EnterTestModeItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/enter_test_mode"));

    private EnterTestModeItem() {
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
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld buildMode)) return;

        FutureUtil.submitVirtual(() -> {
            player.removeTag(SPECTATOR_CHECKPOINT);
            buildMode.enterTestMode(player);
        });
    }

}
