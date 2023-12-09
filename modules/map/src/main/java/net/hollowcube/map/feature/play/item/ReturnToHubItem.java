package net.hollowcube.map.feature.play.item;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class ReturnToHubItem extends ItemHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReturnToHubItem.class);

    public static final String ID = "mapmaker:return_to_hub";
    public static final ReturnToHubItem INSTANCE = new ReturnToHubItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/return_to_hub"));

    private ReturnToHubItem() {
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

        Thread.startVirtualThread(() -> {
            try {
                //player.sendMessage("Returning to hub");
                player.removeTag(SPECTATOR_CHECKPOINT);
                if (world instanceof InternalMapWorld internalWorld) {
                    internalWorld.removePlayer(player);
                }
                world.server().bridge().sendPlayerToHub(player);
            } catch (Exception e) {
                logger.error("failed to send player {} to hub: {}", player.getUuid(), e.getMessage());
                LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                        .forEach(player::sendMessage);
            }
        });
    }

}
