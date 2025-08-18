package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorAnvil;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ActionEditorView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record ResetHeightAction(
        int value
) implements Action {
    private static final int NO_RESET_HEIGHT = Integer.MIN_VALUE;

    private static final Sprite SPRITE = new Sprite("action/icon/reset_height", 3, 3);

    public static final Key KEY = Key.key("mapmaker:reset_height");
    public static final StructCodec<ResetHeightAction> CODEC = StructCodec.struct(
            "value", Codec.INT.optional(NO_RESET_HEIGHT), ResetHeightAction::value,
            ResetHeightAction::new);
    public static final Editor<ResetHeightAction> EDITOR = new Editor<>(
            ResetHeightAction::makeEditor, _ -> SPRITE,
            ResetHeightAction::makeThumbnail, Set.of(ResetHeightAction.KEY));

    private static final int DEFAULT_RESET_HEIGHT_OFFSET = 5;
    public static final Tag<Integer> DEFAULT_RESET_HEIGHT = Tag.Integer("mapmaker:play/reset_height")
            .defaultValue(-64 - DEFAULT_RESET_HEIGHT_OFFSET);

    public ResetHeightAction withValue(int value) {
        return new ResetHeightAction(value);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        state.set(Attachments.RESET_HEIGHT, value == NO_RESET_HEIGHT ? null : value);
    }

    private static TranslatableComponent makeThumbnail(@Nullable ResetHeightAction action) {
        return action == null || action.value == NO_RESET_HEIGHT
                ? Component.translatable("gui.action.reset_height.thumbnail.clear")
                : Component.translatable("gui.action.reset_height.thumbnail", List.of(
                Component.text(action.value)
        ));
    }

    private static Panel makeEditor(ActionList.Ref ref) {
        return new ActionEditorAnvil<>(ref, ResetHeightAction::valueToString, ResetHeightAction::stringToValue) {
            @Override
            protected boolean validateResult(ResetHeightAction result) {
                var actionLocation = Objects.requireNonNull(host.getTag(ActionEditorView.ACTION_LOCATION), "action location");
                int maxResetHeight = actionLocation.blockY();
                var teleport = ref.parent().findLast(TeleportAction.class);
                if (teleport != null) maxResetHeight = Math.max(maxResetHeight,
                        teleport.target().resolve(Pos.fromPoint(actionLocation)).blockY());

                if (result.value < -64) {
                    host.player().sendMessage(Component.translatable("create_maps.checkpoint.reset_height.too_low"));
                    host.player().closeInventory();
                    return false;
                } else if (result.value > maxResetHeight) {
                    host.player().sendMessage(Component.translatable("create_maps.checkpoint.reset_height.too_high",
                            Component.text(result.value), Component.text(maxResetHeight)));
                    host.player().closeInventory();
                    return false;
                } else return true;
            }
        };
    }

    private static String valueToString(ResetHeightAction action) {
        return action.value == NO_RESET_HEIGHT ? "" : String.valueOf(action.value);
    }

    private static ResetHeightAction stringToValue(ResetHeightAction action, String value) {
        if (value.isEmpty()) return action.withValue(NO_RESET_HEIGHT);
        return action.withValue((int) Double.parseDouble(value));
    }

}
