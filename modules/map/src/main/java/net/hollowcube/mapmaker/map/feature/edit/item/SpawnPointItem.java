package net.hollowcube.mapmaker.map.feature.edit.item;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SpawnPointItem extends ItemHandler {
    private static final Logger logger = LoggerFactory.getLogger(SpawnPointItem.class);

    public static final String ID = "mapmaker:spawn_point";
    public static final SpawnPointItem INSTANCE = new SpawnPointItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/spawn_point"));

    private SpawnPointItem() {
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
        if (world == null) return;

        if (player.isSneaking()) {
            if (!world.canEdit(player)) return;
            updateSpawnPoint(player, world.map());
        } else {
            teleportToSpawn(player, world.map());
        }
    }

    private void teleportToSpawn(@NotNull Player player, @NotNull MapData map) {
        player.teleport(map.settings().getSpawnPoint());
        player.sendMessage(Component.translatable("teleport.spawn"));
    }

    private void updateSpawnPoint(@NotNull Player player, @NotNull MapData map) {
        var newSpawnPoint = player.getPosition();
        if (!player.getInstance().getWorldBorder().isInside(newSpawnPoint)) {
            player.sendMessage(Component.translatable("command.set_spawn.out_of_world"));
            return;
        }

        map.settings().setSpawnPoint(newSpawnPoint);
        player.sendMessage(MapMessages.COMMAND_SETSPAWN_SUCCESS.with(
                Component.text(newSpawnPoint.blockX()).hoverEvent(Component.text(newSpawnPoint.x(), NamedTextColor.WHITE)),
                Component.text(newSpawnPoint.blockY()).hoverEvent(Component.text(newSpawnPoint.y(), NamedTextColor.WHITE)),
                Component.text(newSpawnPoint.blockZ()).hoverEvent(Component.text(newSpawnPoint.z(), NamedTextColor.WHITE)),
                Component.text(Math.floor(newSpawnPoint.pitch())).hoverEvent(Component.text(newSpawnPoint.pitch(), NamedTextColor.WHITE)),
                Component.text(Math.floor(newSpawnPoint.yaw())).hoverEvent(Component.text(newSpawnPoint.yaw(), NamedTextColor.WHITE))
        ));
    }

}
