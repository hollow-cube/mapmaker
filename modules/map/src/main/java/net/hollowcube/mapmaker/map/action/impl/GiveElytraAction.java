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

import java.util.Set;

public record GiveElytraAction() implements Action {
    private static final Sprite SPRITE = new Sprite("action/icon/elytra_add", 2, 3);

    public static final Key KEY = Key.key("mapmaker:give_elytra");
    public static final StructCodec<GiveElytraAction> CODEC = StructCodec.struct(GiveElytraAction::new);
    public static final Editor<GiveElytraAction> EDITOR = new Editor<>(null, _ -> SPRITE,
            GiveElytraAction::makeThumbnail, Set.of(GiveElytraAction.KEY, TakeElytraAction.KEY));

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        state.set(Attachments.ELYTRA, true);
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable GiveElytraAction action) {
        return Component.translatable("gui.action.give_elytra.thumbnail");
    }

}
