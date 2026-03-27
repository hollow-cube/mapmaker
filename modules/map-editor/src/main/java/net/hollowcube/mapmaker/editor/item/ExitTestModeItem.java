package net.hollowcube.mapmaker.editor.item;

import net.hollowcube.mapmaker.editor.EditorMapWorld;
import net.hollowcube.mapmaker.editor.EditorState;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ExitTestModeItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/exit_test_mode"));
    public static final Key ID = Key.key("mapmaker:exit_test_mode");
    public static final ExitTestModeItem INSTANCE = new ExitTestModeItem();

    private ExitTestModeItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = EditorMapWorld.forPlayer(player);
        if (world == null || !(world.getPlayerState(player) instanceof EditorState.Testing(var saveState)))
            return;

        world.changePlayerState(player, new EditorState.Building(saveState));
    }

}
