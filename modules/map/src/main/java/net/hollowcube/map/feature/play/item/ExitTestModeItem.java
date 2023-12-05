package net.hollowcube.map.feature.play.item;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class ExitTestModeItem extends ItemHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExitTestModeItem.class);

    public static final String ID = "mapmaker:exit_test_mode";
    public static final ExitTestModeItem INSTANCE = new ExitTestModeItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/exit_test_mode"));

    private ExitTestModeItem() {
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

        try {
            player.removeTag(SPECTATOR_CHECKPOINT);
            if (world instanceof TestingMapWorld internalWorld) {
                internalWorld.exitTestMode(player);
            }
        } catch (Exception e) {
            logger.error("failed to return player {} to build mode: {}", player.getUuid(), e.getMessage());
            LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                    .forEach(player::sendMessage);
        }
    }

}
