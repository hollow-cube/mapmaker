package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.map.item.checkpoint.CheckpointItem;
import net.hollowcube.mapmaker.map.item.checkpoint.CheckpointItems;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public record GiveItemAction(
        @Nullable CheckpointItem item,
        int slot // 0-2 for now
) implements Action {
    private static final Sprite SPRITE_ADD = new Sprite("action/icon/hand_plus", 2, 3);

    public static final Key KEY = Key.key("mapmaker:give_item");
    public static final StructCodec<GiveItemAction> CODEC = StructCodec.struct(
            StructCodec.INLINE, CheckpointItems.CODEC.optional(), GiveItemAction::item,
            "slot", ExtraCodecs.clamppedInt(0, 2).optional(0), GiveItemAction::slot,
            GiveItemAction::new);
    public static final Editor<GiveItemAction> EDITOR = new Editor<>(
            GiveItemAction::createEditor, SPRITE_ADD, GiveItemAction::thumbnail);

    public @NotNull GiveItemAction withItem(@Nullable CheckpointItem item) {
        return new GiveItemAction(item, this.slot);
    }

    public @NotNull GiveItemAction withSlot(int slot) {
        return new GiveItemAction(this.item, Math.clamp(slot, 0, 2));
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        if (this.item == null) return;
        var items = state.get(Attachments.HOTBAR_ITEMS, HotbarItems.EMPTY);
        state.set(Attachments.HOTBAR_ITEMS, items.withItem(this.slot, this.item));
    }

    private static @NotNull TranslatableComponent thumbnail(@Nullable GiveItemAction action) {
        if (action == null || action.item == null)
            return Component.translatable("gui.action.give_item.thumbnail.empty");
        var inner = action.item.thumbnail();
        var args = new ArrayList<>(inner.arguments());
        args.addFirst(TranslationArgument.numeric(action.slot + 1));
        return Component.translatable(inner.key(), args);
    }

    private static @NotNull AbstractActionEditorPanel<GiveItemAction> createEditor(@NotNull ActionList.Ref ref) {
        var item = ref.<GiveItemAction>cast().item;
        return item == null ? new ItemPicker(ref) : item.createEditor(ref);
    }

    private static class ItemPicker extends AbstractActionEditorPanel<GiveItemAction> {
        public ItemPicker(@NotNull ActionList.Ref ref) {
            super(ref);

            background("action/editor/list_container", -10, -31);

            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose item"));

            Consumer<Key> innerUpdateFunc = update((data, item) ->
                    data.withItem(CheckpointItems.createDefault(item)));
            Consumer<Key> updateFunc = key -> {
                innerUpdateFunc.accept(key);
                host.replaceView(ref.<GiveItemAction>cast().item.createEditor(ref));
            };

            int i = 0;
            for (var itemType : CheckpointItems.keys()) {
                int x = i % 7, y = i / 7;

                add(x + 1, y + 2, new Button("gui.action.give_item." + itemType.value(), 1, 1)
                        .model(itemType.asString(), null)
                        .onLeftClick(() -> updateFunc.accept(itemType)));
                i++;
            }
        }

        @Override
        protected void update(@NotNull GiveItemAction data) {
            // Do nothing, we pop the view when selecting an item
        }
    }

    public static abstract class AbstractItemEditor<T extends CheckpointItem> extends AbstractActionEditorPanel<GiveItemAction> {

        private enum Slot {
            ONE, TWO, THREE; // We just use indices but multi select uses an enum for something idk
        }

        private final ControlledTriStateInput<Slot> slotInput;

        public AbstractItemEditor(@NotNull ActionList.Ref ref) {
            this(ref, false);
        }

        public AbstractItemEditor(@NotNull ActionList.Ref ref, boolean oneSlotLessHack) {
            super(ref, oneSlotLessHack);

            // Should be non-null now because we otherwise open the picker
            var key = CheckpointItems.getKey(Objects.requireNonNull(ref.<GiveItemAction>cast().item));
            subtitleText.text(LanguageProviderV2.translateToPlain("gui.action.give_item." + key.value() + ".title"));


            this.slotInput = add(1, 1, new ControlledTriStateInput<>("give_item", Slot.class,
                    update((data, op) -> data.withSlot(op.ordinal())))
                    .label("choose slot").labels("slot0", "slot1", "slot2"));
            this.slotInput.update(Slot.ONE);
            this.slotInput.iconButton().onLeftClick(() -> {
                host.replaceView(new ItemPicker(ref));
            });
        }

        @Override
        @SuppressWarnings("unchecked")
        protected final void update(@NotNull GiveItemAction data) {
            if (data.item == null) return;

            this.slotInput.update(Slot.values()[data.slot]);
            var iconButton = this.slotInput.iconButton();
            var iconStack = data.item.createItemStack();
            iconButton.model(iconStack.get(DataComponents.ITEM_MODEL), null);
            iconButton.text(Component.text("").decoration(TextDecoration.ITALIC, false)
                    .append(iconStack.get(DataComponents.CUSTOM_NAME, iconStack.get(DataComponents.ITEM_NAME))), List.of());
            iconButton.extraComponents(iconStack.componentPatch());

            updateItem((T) data.item);
        }

        protected abstract void updateItem(@NotNull T item);

        protected <V> @NotNull Consumer<V> updateItem(@NotNull BiFunction<T, V, T> setter) {
            return update((data, value) -> data.withItem(setter.apply((T) data.item, value)));
        }

        protected ControlledNumberInput makeGenericAmount(@NotNull BiFunction<T, Integer, T> setter) {
            return new ControlledNumberInput("give_item.generic.amount", updateItem(setter))
                    .formatted(i -> i == 0 ? "Infinite" : String.valueOf(i))
                    .range(0, 99).stepped(1, 5);
        }

        protected ControlledNumberInput makeGenericCooldown(@NotNull BiFunction<T, Integer, T> setter, boolean oneSlotHack) {
            return new ControlledNumberInput("give_item.generic.cooldown", updateItem(setter), oneSlotHack)
                    .parsed(dur -> NumberUtil.formatDuration(dur * 50L), NumberUtil::parseDurationToTicks)
                    .formatted(dur -> NumberUtil.formatDuration(dur * 50L))
                    .range(1, 24 * 60 * 60 * 20) // 1h in ticks
                    .stepped(10, 20); // 0.5s, 1s steps
        }
    }
}
