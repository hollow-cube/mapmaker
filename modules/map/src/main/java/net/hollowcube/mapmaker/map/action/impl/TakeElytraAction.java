package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record TakeElytraAction() implements Action {
    private static final Sprite SPRITE = new Sprite("action/icon/elytra_add", 2, 2);

    public static final Key KEY = Key.key("mapmaker:take_elytra");
    public static final StructCodec<TakeElytraAction> CODEC = StructCodec.struct(TakeElytraAction::new);
    public static final Editor<TakeElytraAction> EDITOR = new Editor<>(null, SPRITE, TakeElytraAction::makeThumbnail);

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        state.set(Attachments.ELYTRA, null);
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable TakeElytraAction action) {
        return Component.translatable("gui.action.take_elytra.thumbnail");
    }

}
