package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.entity.potion.PotionInfo;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static net.kyori.adventure.text.Component.translatable;

public record AddPotionAction(
        @Nullable PotionInfo effect, int level, int duration
) implements Action {
    private static final int INFINITE_DURATION = 0;
    private static final int MAX_DURATION = 24 * 60 * 60 * 20; // 24 hours in ticks.

    private static final Sprite SPRITE_ADD = new Sprite("action/icon/potion_add", 2, 2);

    public static final Key KEY = Key.key("mapmaker:add_potion");
    public static final StructCodec<AddPotionAction> CODEC = StructCodec.struct(
            "effect", PotionInfo.CODEC.optional(), AddPotionAction::effect,
            "level", ExtraCodecs.clamppedInt(0, 128).optional(1), AddPotionAction::level,
            "duration", ExtraCodecs.clamppedInt(INFINITE_DURATION, MAX_DURATION).optional(INFINITE_DURATION), AddPotionAction::duration,
            AddPotionAction::new);
    public static final Action.Editor<AddPotionAction> EDITOR = new Action.Editor<>(
            AddPotionAction::createEditor, SPRITE_ADD, AddPotionAction::thumbnail);

    public @NotNull AddPotionAction withEffect(@Nullable PotionInfo effect) {
        return new AddPotionAction(effect, this.level, this.duration);
    }

    public @NotNull AddPotionAction withLevel(int level) {
        return new AddPotionAction(this.effect, level, this.duration);
    }

    public @NotNull AddPotionAction withDuration(int duration) {
        return new AddPotionAction(this.effect, this.level, duration);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        if (effect == null) return;

        // TODO: sound effects
        var potionEffects = state.get(Attachments.POTION_EFFECTS);
        if (potionEffects == null) {
            potionEffects = new PotionEffectList();
            state.set(Attachments.POTION_EFFECTS, potionEffects);
        }
        var existingEffect = potionEffects.getOrCreate(effect);
        existingEffect.setLevel(level);
        existingEffect.setDuration(duration * 50);
    }

    private static @NotNull TranslatableComponent thumbnail(@Nullable AddPotionAction action) {
        if (action == null || action.effect == null)
            return Component.translatable("gui.action.add_potion.thumbnail.empty");
        return Component.translatable("gui.action.add_potion.thumbnail", List.of(
                Component.translatable(action.effect.translationKey() + ".name"),
                Component.text(action.level),
                Component.text(action.duration == 0 ? "Infinite" : NumberUtil.formatDuration(action.duration * 50L))
        ));
    }

    private static @NotNull AbstractActionEditorPanel<AddPotionAction> createEditor(@NotNull ActionList.Ref ref) {
        return ref.<AddPotionAction>cast().effect == null
                ? new PotionEffectPickerView(ref)
                : new PotionEffectEditorView(ref);
    }

    private static class PotionEffectPickerView extends AbstractActionEditorPanel<AddPotionAction> {
        public PotionEffectPickerView(@NotNull ActionList.Ref ref) {
            super(ref);

            background("action/editor/list_container", -10, -31);

            add(1, 1, net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel.groupText(7, "choose effect"));

            var updateFunc = update(AddPotionAction::withEffect);
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
        protected void update(@NotNull AddPotionAction data) {
            if (data.effect == null) return;
            host.replaceView(new PotionEffectEditorView(ref));
        }
    }

    private static class PotionEffectEditorView extends AbstractActionEditorPanel<AddPotionAction> {
        private final ControlledNumberInput levelInput;
        private final ControlledNumberInput durationInput;

        public PotionEffectEditorView(@NotNull ActionList.Ref ref) {
            super(ref);

            // Should be non-null now because we otherwise open the picker
            var effect = Objects.requireNonNull(ref.<AddPotionAction>cast().effect());
            subtitleText.text(LanguageProviderV2.translateToPlain(translatable(effect.translationKey() + ".name")));

            this.levelInput = add(1, 1, new ControlledNumberInput("add_potion.level", update(AddPotionAction::withLevel))
                    .range(1, effect.maxLevel()));
            this.durationInput = add(1, 3, new ControlledNumberInput("add_potion.duration", update(AddPotionAction::withDuration))
                    .parsed(i -> i == 0 ? "" : String.valueOf(i / 20.), NumberUtil::parseDurationToTicks)
                    .formatted(i -> i == 0 ? "Infinite" : NumberUtil.formatDuration(i * 50L))
                    .range(INFINITE_DURATION, MAX_DURATION).stepped(5 * 20, 3 * 20));
        }

        @Override
        protected void update(@NotNull AddPotionAction data) {
            levelInput.update(data.level);
            durationInput.update(data.duration);
        }
    }
}
