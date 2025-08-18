package net.hollowcube.mapmaker.editor.item;

import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.CoordinateUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.kyori.adventure.text.Component.translatable;

public class SpawnPointItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/spawn_point"));

    public static final Key ID = Key.key("mapmaker:spawn_point");
    public static final SpawnPointItem INSTANCE = new SpawnPointItem();

    private SpawnPointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = EditorMapWorld.forPlayer(player);
        if (world == null) return;

        if (player.isSneaking()) {
            updateSpawnPoint(player, world);
        } else {
            teleportToSpawn(player, world.map());
        }
    }

    private void teleportToSpawn(@NotNull Player player, @NotNull MapData map) {
        player.teleport(map.settings().getSpawnPoint(), Vec.ZERO, null, RelativeFlags.NONE);
        player.sendMessage(Component.translatable("teleport.spawn"));
    }

    private void updateSpawnPoint(@NotNull Player player, @NotNull EditorMapWorld world) {
        var newSpawnPoint = player.getPosition();
        if (!player.getInstance().getWorldBorder().inBounds(newSpawnPoint)) {
            player.sendMessage(Component.translatable("command.set_spawn.out_of_world"));
            return;
        }

        world.setSpawnPoint(newSpawnPoint);
        player.sendMessage(translatable("command.set_spawn.success", CoordinateUtil.asTranslationArgs(newSpawnPoint)));
    }

}
