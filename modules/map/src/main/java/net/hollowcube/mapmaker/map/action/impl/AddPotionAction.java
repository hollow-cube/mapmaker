package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.entity.potion.PotionInfo;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.kyori.adventure.text.Component.translatable;

public class AddPotionAction extends AbstractAction<AddPotionAction.Data> {
    public static final AddPotionAction INSTANCE = new AddPotionAction();

    private static final int INFINITE_DURATION = 0;
    private static final int MAX_DURATION = 24 * 60 * 60 * 20; // 24 hours in ticks.

    private static final Sprite SPRITE_ADD = new Sprite("action/icon/potion_add", 2, 2);

    public record Data(@Nullable PotionInfo effect, int level, int duration) {
        public static final StructCodec<Data> CODEC = StructCodec.struct(
                "effect", PotionInfo.CODEC.optional(), Data::effect,
                "level", ExtraCodecs.clamppedInt(0, 128).optional(0), Data::level,
                "duration", ExtraCodecs.clamppedInt(INFINITE_DURATION, MAX_DURATION), Data::duration,
                Data::new);

        public @NotNull Data withEffect(@Nullable PotionInfo effect) {
            return new Data(effect, this.level, this.duration);
        }

        public @NotNull Data withLevel(int level) {
            return new Data(this.effect, level, this.duration);
        }

        public @NotNull Data withDuration(int duration) {
            return new Data(this.effect, this.level, duration);
        }
    }

    public AddPotionAction() {
        super("mapmaker:add_potion", Data.CODEC, new Data(null, 1, INFINITE_DURATION));
    }

    @Override
    public @NotNull Sprite sprite(@Nullable Data data) {
        return SPRITE_ADD;
    }

    @Override
    public @NotNull AbstractActionEditorPanel<Data> createEditor(@NotNull ActionList.ActionData<Data> actionData) {
        return actionData.getData().effect() == null
                ? new PotionEffectPickerView(actionData)
                : new PotionEffectEditorView(actionData);
    }

    private static class PotionEffectPickerView extends AbstractActionEditorPanel<Data> {
        public PotionEffectPickerView(@NotNull ActionList.ActionData<Data> actionData) {
            super(actionData);
            this.isTransient = true;

            background("action/editor/list_container", -10, -31);

            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose effect"));

            var updateFunc = update(Data::withEffect);
            var potionTypes = PotionInfo.sortedValues();
            for (int i = 0; i < potionTypes.size(); i++) {
                var potionInfo = potionTypes.get(i);
                int x = i % 7, y = i / 7;

                add(x + 1, y + 2, new Button(potionInfo.translationKey(), 1, 1)
                        .model(potionInfo.itemModel(), null)
                        .onLeftClick(() -> updateFunc.accept(potionInfo)));
            }
        }

        @Override
        protected void update(@NotNull Data data) {
            if (data.effect == null) return;
            host.pushView(new PotionEffectEditorView(actionData));
        }
    }

    private static class PotionEffectEditorView extends AbstractActionEditorPanel<Data> {
        private final ControlledNumberInput levelInput;
        private final ControlledNumberInput durationInput;

        public PotionEffectEditorView(@NotNull ActionList.ActionData<Data> actionData) {
            super(actionData);

            // Should be non-null now because we otherwise open the picker
            var effect = Objects.requireNonNull(actionData.getData().effect());
            subtitleText.text(LanguageProviderV2.translateToPlain(translatable(effect.translationKey() + ".name")));
            this.levelInput = add(1, 1, new ControlledNumberInput(update(Data::withLevel))
                    .label("potion level").range(1, effect.maxLevel()));
            this.durationInput = add(1, 3, new ControlledNumberInput(update(Data::withDuration))
                    .formatted(i -> i == 0 ? "Infinite" : NumberUtil.formatDuration(i * 50L))
                    .label("potion duration").range(INFINITE_DURATION, MAX_DURATION));
        }

        @Override
        protected void update(@NotNull Data data) {
            levelInput.update(data.level);
            durationInput.update(data.duration);
        }
    }
}
