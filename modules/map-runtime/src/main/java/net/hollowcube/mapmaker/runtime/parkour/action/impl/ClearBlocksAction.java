package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public record ClearBlocksAction() implements Action {
    private static final Sprite SPRITE = new Sprite("action/icon/trash", 3, 3);

    public static final Key KEY = Key.key("mapmaker:clear_blocks");
    public static final StructCodec<ClearBlocksAction> CODEC = StructCodec.struct(ClearBlocksAction::new);
    public static final Editor<ClearBlocksAction> EDITOR = new Editor<>(null, _ -> SPRITE,
            ClearBlocksAction::makeThumbnail, Set.of(ClearBlocksAction.KEY));

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        // The holder is reloaded from the play state right after actions are applied, so clearing
        // it directly would immediately be undone. Clear the state and let the reload do the work.
        state.setGhostBlocks(Map.of());
    }

    private static TranslatableComponent makeThumbnail(@Nullable ClearBlocksAction action) {
        return Component.translatable("gui.action.clear_blocks.thumbnail");
    }

}
