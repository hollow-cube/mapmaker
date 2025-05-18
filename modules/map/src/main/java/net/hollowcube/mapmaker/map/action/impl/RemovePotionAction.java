package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.entity.potion.PotionInfo;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class RemovePotionAction extends AbstractAction<RemovePotionAction.Data> {
    public static final RemovePotionAction INSTANCE = new RemovePotionAction();

    private static final Sprite SPRITE_SUBTRACT = new Sprite("action/icon/potion_subtract", 2, 2);
    private static final Sprite SPRITE_CLEAR = new Sprite("action/icon/potion_clear", 2, 2);

    public record Data(@Nullable PotionInfo effect) {
        // Null means to remove all effects
        public static final StructCodec<Data> CODEC = StructCodec.struct(
                "effect", PotionInfo.CODEC.optional(), Data::effect,
                Data::new);

        public @NotNull Data withEffect(@Nullable PotionInfo effect) {
            return new Data(effect);
        }
    }

    public RemovePotionAction() {
        super("mapmaker:remove_potion", Data.CODEC, new Data(null));
    }

    @Override
    public @NotNull Sprite sprite(@Nullable Data data) {
        return data == null || data.effect() != null
                ? SPRITE_SUBTRACT : SPRITE_CLEAR;
    }

    @Override
    public @NotNull AbstractActionEditorPanel<Data> createEditor(@NotNull ActionList.ActionData<Data> actionData) {
        return new PotionEffectPickerView(actionData);
    }

    private static class PotionEffectPickerView extends AbstractActionEditorPanel<Data> {
        public PotionEffectPickerView(@NotNull ActionList.ActionData<Data> actionData) {
            super(actionData);
            this.isTransient = true;

            background("action/editor/list_container", -10, -31);
            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose effect"));

            Consumer<PotionInfo> innerUpdateFunc = update(Data::withEffect);
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
            add(7, 3, new Button("remove_all", 1, 1)
                    .sprite("action/icon/milk_bucket", 3, 2)
                    .onLeftClick(() -> updateFunc.accept(null)));
        }

        @Override
        protected void update(@NotNull Data data) {
            // Noop, we just pop the view when updating
        }
    }

}
