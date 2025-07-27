package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.HotbarItems;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledTriStateInput;
import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.BlockCheckpointItem;
import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.CheckpointItem;
import net.hollowcube.mapmaker.runtime.parkour.item.checkpoint.CheckpointItems;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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

    public GiveItemAction withItem(@Nullable CheckpointItem item) {
        return new GiveItemAction(item, this.slot);
    }

    public GiveItemAction withSlot(int slot) {
        return new GiveItemAction(this.item, Math.clamp(slot, 0, 2));
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        if (this.item == null) return;
        var items = state.get(Attachments.HOTBAR_ITEMS, HotbarItems.EMPTY);
        state.set(Attachments.HOTBAR_ITEMS, items.withItem(this.slot, this.item));
    }

    private static TranslatableComponent thumbnail(@Nullable GiveItemAction action) {
        if (action == null || action.item == null)
            return Component.translatable("gui.action.give_item.thumbnail.empty");
        var inner = action.item.thumbnail();
        var args = new ArrayList<>(inner.arguments());
        args.addFirst(TranslationArgument.numeric(action.slot + 1));
        return Component.translatable(inner.key(), args);
    }

    private static AbstractActionEditorPanel<GiveItemAction> createEditor(ActionList.Ref ref) {
        var item = ref.<GiveItemAction>cast().item;
        return item == null ? new ItemPicker(ref) : item.createEditor(ref);
    }

    private static class ItemPicker extends AbstractActionEditorPanel<GiveItemAction> {
        public ItemPicker(ActionList.Ref ref) {
            super(ref);

            background("action/editor/list_container", -10, -31);

            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose item"));

        }

        @Override
        protected void mount(InventoryHost host, boolean isInitial) {
            super.mount(host, isInitial);

            if (!isInitial) return;

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
                        // TODO: generalize this handling rather than hardcoding to this specific behavior for block items
                        .model(itemType.equals(BlockCheckpointItem.ID) ? "minecraft:stone" : itemType.asString(), null)
                        .onLeftClick(() -> updateFunc.accept(itemType)));
                i++;
            }
        }

        @Override
        protected void update(GiveItemAction data) {
            // Do nothing, we pop the view when selecting an item
        }
    }

    public static abstract class AbstractItemEditor<T extends CheckpointItem> extends AbstractActionEditorPanel<GiveItemAction> {

        private enum Slot {
            ONE, TWO, THREE; // We just use indices but multi select uses an enum for something idk
        }

        protected final ControlledTriStateInput<Slot> slotInput;

        public AbstractItemEditor(ActionList.Ref ref) {
            this(ref, false);
        }

        public AbstractItemEditor(ActionList.Ref ref, boolean oneSlotLessHack) {
            super(ref, oneSlotLessHack);

            // Should be non-null now because we otherwise open the picker
            var key = CheckpointItems.getKey(Objects.requireNonNull(ref.<GiveItemAction>cast().item));
            subtitleText.text(LanguageProviderV2.translateToPlain("gui.action.give_item." + key.value() + ".title"));

            this.slotInput = add(1, 1, new ControlledTriStateInput<>("give_item", Slot.class,
                    update((data, op) -> data.withSlot(op.ordinal())))
                    .label("choose slot").labels("slot0", "slot1", "slot2"));
            this.slotInput.update(Slot.ONE);
            this.slotInput.iconButton().onLeftClick(() -> host.replaceView(new ItemPicker(ref)));
            this.slotInput.iconButton().onShiftLeftClick(() -> host.replaceView(new ItemPicker(ref)));
        }

        @Override
        @SuppressWarnings("unchecked")
        protected final void update(GiveItemAction data) {
            if (data.item == null) return;

            this.slotInput.update(Slot.values()[data.slot]);
            this.slotInput.iconButton().from(data.item.createItemStack());

            updateItem((T) data.item);
        }

        protected abstract void updateItem(T item);

        protected <V> Consumer<V> updateItem(BiFunction<T, V, T> setter) {
            return update((data, value) -> data.withItem(setter.apply((T) data.item, value)));
        }

        protected ControlledNumberInput makeGenericAmount(BiFunction<T, Integer, T> setter, int maxAmount) {
            return new ControlledNumberInput("give_item.generic.amount", updateItem(setter))
                    .formatted(i -> i == 0 ? "Infinite" : String.valueOf(i))
                    .range(0, 99).stepped(1, 5);
        }

        protected ControlledNumberInput makeGenericCooldown(BiFunction<T, Integer, T> setter, boolean oneSlotHack) {
            return new ControlledNumberInput("give_item.generic.cooldown", updateItem(setter), oneSlotHack)
                    .parsed(dur -> NumberUtil.formatDuration(dur * 50L), NumberUtil::parseDurationToTicks)
                    .formatted(dur -> NumberUtil.formatDuration(dur * 50L))
                    .range(1, 24 * 60 * 60 * 20) // 1h in ticks
                    .stepped(10, 20); // 0.5s, 1s steps
        }
    }
}
