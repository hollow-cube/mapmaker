package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record TakeElytraAction() implements Action {
    private static final Sprite SPRITE = new Sprite("action/icon/elytra_subtract", 2, 3);

    public static final Key KEY = Key.key("mapmaker:take_elytra");
    public static final StructCodec<TakeElytraAction> CODEC = StructCodec.struct(TakeElytraAction::new);
    public static final Editor<TakeElytraAction> EDITOR = new Editor<>(null, _ -> SPRITE,
            TakeElytraAction::makeThumbnail, Set.of(GiveElytraAction.KEY, TakeElytraAction.KEY));

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        state.set(Attachments.ELYTRA, null);
    }

    private static TranslatableComponent makeThumbnail(@Nullable TakeElytraAction action) {
        return Component.translatable("gui.action.take_elytra.thumbnail");
    }

}
