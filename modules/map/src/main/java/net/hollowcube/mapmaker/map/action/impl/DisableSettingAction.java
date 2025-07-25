package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.action.Action;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.feature.play.setting.SavedMapSettings;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.translatable;

/// Only supports boolean settings for now, should revisit later.
public record DisableSettingAction(
        // Null has no effect when executed.
        @Nullable MapSetting<?> setting
) implements Action {
    private static final Sprite SPRITE = new Sprite("action/icon/setting_subtract", 2, 3);

    public static final Key KEY = Key.key("mapmaker:disable_setting");
    public static final StructCodec<DisableSettingAction> CODEC = StructCodec.struct(
            "setting", MapSetting.CODEC.optional(), DisableSettingAction::setting,
            DisableSettingAction::new);
    public static final Action.Editor<DisableSettingAction> EDITOR = new Action.Editor<>(
            Editor::new, SPRITE, DisableSettingAction::makeThumbnail);

    public @NotNull DisableSettingAction withSetting(@Nullable MapSetting<?> setting) {
        return new DisableSettingAction(setting);
    }

    @Override
    public @NotNull StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(@NotNull Player player, @NotNull PlayState state) {
        var settings = state.get(Attachments.SETTINGS);
        if (settings == null) settings = new SavedMapSettings();
        if (setting == null || !(setting.defaultValue() instanceof Boolean)) return;
        state.set(Attachments.SETTINGS, settings.with((MapSetting<Boolean>) setting, false));
    }

    private static @NotNull TranslatableComponent makeThumbnail(@Nullable DisableSettingAction action) {
        if (action == null || action.setting == null)
            return translatable("gui.action.disable_setting.thumbnail.empty");
        return translatable("gui.action.disable_setting.thumbnail", List.of(
                translatable(EnableSettingAction.SETTINGS_TRANSLATION_KEYS.get(action.setting) + ".name")
        ));
    }

    private static class Editor extends AbstractActionEditorPanel<DisableSettingAction> {
        public Editor(@NotNull ActionList.Ref ref) {
            super(ref);

            background("action/editor/list_container", -10, -31);
            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose setting"));

            Consumer<MapSetting<?>> innerUpdateFunc = update(DisableSettingAction::withSetting);
            Consumer<MapSetting<?>> updateFunc = setting -> {
                innerUpdateFunc.accept(setting);
                host.popView();
            };

            for (int i = 0; i < EnableSettingAction.BOOL_SETTINGS.size(); i++) {
                var setting = EnableSettingAction.BOOL_SETTINGS.get(i);
                int x = i % 7, y = i / 7;

                add(x + 1, y + 2, new Button(EnableSettingAction.SETTINGS_TRANSLATION_KEYS.get(setting), 1, 1)
                        .model(EnableSettingAction.SETTINGS_ICONS.get(setting), null)
                        .onLeftClick(() -> updateFunc.accept(setting)));
            }
        }

        @Override
        protected void update(@NotNull DisableSettingAction data) {
            // Noop, we just pop the view when updating
        }
    }

}
