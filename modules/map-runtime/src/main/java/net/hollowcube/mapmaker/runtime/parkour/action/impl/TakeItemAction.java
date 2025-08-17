package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.HotbarItems;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
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

    public TakeItemAction withSlot0(boolean slot0) {
        return new TakeItemAction(slot0, this.slot1, this.slot2);
    }

    public TakeItemAction withSlot1(boolean slot1) {
        return new TakeItemAction(this.slot0, slot1, this.slot2);
    }

    public TakeItemAction withSlot2(boolean slot2) {
        return new TakeItemAction(this.slot0, this.slot1, slot2);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        if (!this.slot0 && !this.slot1 && !this.slot2) return;
        var items = state.get(Attachments.HOTBAR_ITEMS, HotbarItems.EMPTY);
        if (this.slot0) items = items.withItem(0, null);
        if (this.slot1) items = items.withItem(1, null);
        if (this.slot2) items = items.withItem(2, null);
        state.set(Attachments.HOTBAR_ITEMS, items);
    }

    private static TranslatableComponent makeThumbnail(@Nullable TakeItemAction action) {
        if (action == null) return Component.translatable("gui.action.take_item.thumbnail.empty");
        var slots = new ArrayList<Component>();
        if (action.slot0) slots.add(Component.text("1", TextColor.color(0xF2F2F2)));
        if (action.slot1) slots.add(Component.text("2", TextColor.color(0xF2F2F2)));
        if (action.slot2) slots.add(Component.text("3", TextColor.color(0xF2F2F2)));
        if (slots.isEmpty()) return Component.translatable("gui.action.take_item.thumbnail.empty");
        var arg0 = Component.join(JoinConfiguration.separator(net.kyori.adventure.text.Component.text(", ", TextColor.color(0xB0B0B0))), slots);
        return Component.translatable("gui.action.take_item.thumbnail", arg0);
    }

    private static class Editor extends AbstractActionEditorPanel<TakeItemAction> {
        private final Button slot0Button;
        private final Button slot1Button;
        private final Button slot2Button;

        public Editor(ActionList.Ref ref) {
            super(ref, true);

            background("action/editor/container_sm", -10, -31);

            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose slots")
                    .translationKey("gui.action.take_item.label"));

            add(1, 2, new Button("gui.action.take_item.label", 1, 1)
                    .background("generic2/btn/tristate/icon", -2, -1)
                    .sprite(SPRITE_SUBTRACT));

            this.slot0Button = add(2, 2, new Text("gui.action.take_item.slot0.label", 2, 1, "Slot 1")
                    .align(Text.CENTER, Text.CENTER))
                    .sprite("generic2/btn/tristate/default")
                    .onLeftClick(() -> update(TakeItemAction::withSlot0).accept(!ref.<TakeItemAction>cast().slot0));
            this.slot1Button = add(4, 2, new Text("gui.action.take_item.slot1.label", 2, 1, "Slot 2")
                    .align(Text.CENTER, Text.CENTER))
                    .sprite("generic2/btn/tristate/default")
                    .onLeftClick(() -> update(TakeItemAction::withSlot1).accept(!ref.<TakeItemAction>cast().slot1));
            this.slot2Button = add(6, 2, new Text("gui.action.take_item.slot2.label", 2, 1, "Slot 3")
                    .align(Text.CENTER, Text.CENTER))
                    .sprite("generic2/btn/tristate/default")
                    .onLeftClick(() -> update(TakeItemAction::withSlot2).accept(!ref.<TakeItemAction>cast().slot2));
        }

        @Override
        protected void update(TakeItemAction data) {
            slot0Button.sprite("generic2/btn/tristate/" + (data.slot0 ? "selected" : "default"));
            slot0Button.lorePostfix(data.slot0 ? null : AbstractActionEditorPanel.LORE_POSTFIX_CLICKSELECT);
            slot1Button.sprite("generic2/btn/tristate/" + (data.slot1 ? "selected" : "default"));
            slot1Button.lorePostfix(data.slot1 ? null : AbstractActionEditorPanel.LORE_POSTFIX_CLICKSELECT);
            slot2Button.sprite("generic2/btn/tristate/" + (data.slot2 ? "selected" : "default"));
            slot2Button.lorePostfix(data.slot2 ? null : AbstractActionEditorPanel.LORE_POSTFIX_CLICKSELECT);
        }
    }

}
