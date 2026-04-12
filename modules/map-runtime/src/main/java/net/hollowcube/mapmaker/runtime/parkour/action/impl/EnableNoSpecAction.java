package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record EnableNoSpecAction() implements Action {
    private static final Sprite SPRITE = new Sprite("icon2/1_1/tv_off_x", 1, 1);

    public static final Key KEY = Key.key("mapmaker:enable_no_spec");
    public static final StructCodec<EnableNoSpecAction> CODEC = StructCodec.struct(EnableNoSpecAction::new);
    public static final Editor<EnableNoSpecAction> EDITOR = new Editor<>(
        null, _ -> SPRITE, EnableNoSpecAction::makeThumbnail,
        Set.of(EnableNoSpecAction.KEY), Permission.GENERIC_STAFF
    );

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        // Does nothing, we check for this in SpectateHelper
    }

    private static TranslatableComponent makeThumbnail(@Nullable EnableNoSpecAction action) {
        return Component.translatable("gui.action.enable_no_spec.thumbnail");
    }

}
