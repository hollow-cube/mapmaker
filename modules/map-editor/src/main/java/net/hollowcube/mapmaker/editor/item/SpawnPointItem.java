package net.hollowcube.mapmaker.editor.item;

import net.hollowcube.mapmaker.editor.CommonEditorActions;
import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.RelativeFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
            CommonEditorActions.trySetSpawn(player, player.getPosition());
        } else {
            player.teleport(world.map().settings().getSpawnPoint(), Vec.ZERO, null, RelativeFlags.NONE);
            player.sendMessage(Component.translatable("teleport.spawn"));
        }
    }

}
