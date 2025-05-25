package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.map.action.util.Operation;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;

public record TakeItemAction(boolean slot0, boolean slot1, boolean slot2) implements Action {
    private static final Sprite SPRITE_SUBTRACT = new Sprite("action/icon/hand_subtract", 2, 3);

    public static final Key KEY = Key.key("mapmaker:take_item");
    public static final StructCodec<TakeItemAction> CODEC = StructCodec.struct(
            "slot0", Codec.BOOLEAN.optional(false), TakeItemAction::slot0,
            "slot1", Codec.BOOLEAN.optional(false), TakeItemAction::slot1,
            "slot2", Codec.BOOLEAN.optional(false), TakeItemAction::slot2,
            TakeItemAction::new);
    public static final Action.Editor<TakeItemAction> EDITOR = new Action.Editor<>(
            TakeItemAction.Editor::new, _ -> SPRITE_SUBTRACT,
            TakeItemAction::makeThumbnail, Set.of());

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {

        // TODO
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable TakeItemAction action) {
        if (action == null) return Component.translatable("gui.action.take_item.thumbnail.empty");
        var slots = new ArrayList<Component>();
        if (action.slot0) slots.add(Component.text("1", TextColor.color(0xF2F2F2)));
        if (action.slot1) slots.add(Component.text("2", TextColor.color(0xF2F2F2)));
        if (action.slot2) slots.add(Component.text("3", TextColor.color(0xF2F2F2)));
        var arg0 = Component.join(JoinConfiguration.separator(net.kyori.adventure.text.Component.text(", ", TextColor.color(0xB0B0B0))), slots);
        return Component.translatable("gui.action.take_item.thumbnail", arg0);
    }

    private static class Editor extends AbstractActionEditorPanel<TakeItemAction> {
        public Editor(@NotNull ActionList.Ref ref) {
            super(ref);

            background("action/editor/container_sm", -10, -31);

            // TODO this needs to be a multi select and actually update its input as expected
            add(1, 1, new ControlledTriStateInput<>("take_item", Operation.class, d -> {
            })
                    .label("choose slot").labels("slot0", "slot1", "slot2")
                    .sprites(SPRITE_SUBTRACT, SPRITE_SUBTRACT, SPRITE_SUBTRACT));
        }

        @Override
        protected void update(@NotNull TakeItemAction data) {
        }
    }

}
