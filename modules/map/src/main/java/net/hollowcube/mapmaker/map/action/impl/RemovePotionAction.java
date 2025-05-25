package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.entity.potion.PotionEffectList;
import net.hollowcube.mapmaker.map.entity.potion.PotionInfo;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public record RemovePotionAction(
        // Null means to remove all effects
        @Nullable PotionInfo effect
) implements Action {
    private static final Sprite SPRITE_SUBTRACT = new Sprite("action/icon/potion_subtract", 2, 2);
    private static final Sprite SPRITE_CLEAR = new Sprite("action/icon/potion_clear", 2, 2);

    public static final Key KEY = Key.key("mapmaker:remove_potion");
    public static final StructCodec<RemovePotionAction> CODEC = StructCodec.struct(
            "effect", PotionInfo.CODEC.optional(), RemovePotionAction::effect,
            RemovePotionAction::new);
    public static final Action.Editor<RemovePotionAction> EDITOR = new Action.Editor<>(
            RemovePotionAction.Editor::new, RemovePotionAction::makeSprite,
            RemovePotionAction::makeThumbnail, Set.of());

    public @NotNull RemovePotionAction withEffect(@Nullable PotionInfo effect) {
        return new RemovePotionAction(effect);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        // todo sound effects only if 1+ effects were actually removed AND an effect was not added.
        var potionEffects = state.get(Attachments.POTION_EFFECTS, new PotionEffectList());
        if (effect == null) {
            potionEffects.clear();
        } else {
            potionEffects.remove(effect);
        }
    }

    private static @NotNull Sprite makeSprite(@Nullable RemovePotionAction action) {
        return action == null || action.effect != null
                ? SPRITE_SUBTRACT : SPRITE_CLEAR;
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable RemovePotionAction action) {
        return action == null || action.effect == null
                ? Component.translatable("gui.action.remove_potion.thumbnail.clear")
                : Component.translatable("gui.action.remove_potion.thumbnail", List.of(
                Component.translatable(action.effect.translationKey() + ".name")
        ));
    }

    private static class Editor extends AbstractActionEditorPanel<RemovePotionAction> {
        public Editor(@NotNull ActionList.Ref ref) {
            super(ref);

            background("action/editor/list_container", -10, -31);
            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose effect"));

            Consumer<PotionInfo> innerUpdateFunc = update(RemovePotionAction::withEffect);
            Consumer<PotionInfo> updateFunc = potionInfo -> {
                innerUpdateFunc.accept(potionInfo);
                host.popView();
            };

            var potionTypes = PotionInfo.sortedValues();
            for (int i = 0; i < potionTypes.size(); i++) {
                var potionInfo = potionTypes.get(i);
                int x = i % 7, y = i / 7;

                add(x + 1, y + 2, new Button(potionInfo.translationKey(), 1, 1)
                        .model(potionInfo.itemModel(), null)
                        .onLeftClick(() -> updateFunc.accept(potionInfo)));
            }

            // Add the remove button at the end
            add(7, 3, new Button("gui.action.remove_potion.clear", 1, 1)
                    .sprite("action/icon/milk_bucket", 3, 2)
                    .onLeftClick(() -> updateFunc.accept(null)));
        }

        @Override
        protected void update(@NotNull RemovePotionAction data) {
            // Noop, we just pop the view when updating
        }
    }

}
