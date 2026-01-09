package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.runtime.parkour.setting.SavedMapSettings;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.translatable;

// Only supports boolean settings for now, should revisit later.
public record EnableSettingAction(
        // Null has no effect when executed.
        @Nullable MapSetting<?> setting
) implements Action {
    static final List<MapSetting<Boolean>> BOOL_SETTINGS = List.of(
            MapSettings.ONLY_SPRINT, MapSettings.NO_SPRINT,
            MapSettings.NO_JUMP, MapSettings.NO_SNEAK,
            MapSettings.RESET_IN_WATER, MapSettings.RESET_IN_LAVA,
            MapSettings.NO_TURN
    );
    // These maps are pretty gross should revisit. They must match the list above
    static final Map<MapSetting<Boolean>, String> SETTINGS_TRANSLATION_KEYS = Map.of(
            MapSettings.ONLY_SPRINT, "gui.create_maps.map_settings_tab.gameplay.only_sprint",
            MapSettings.NO_SPRINT, "gui.create_maps.map_settings_tab.gameplay.no_sprint",
            MapSettings.NO_JUMP, "gui.create_maps.map_settings_tab.gameplay.no_jump",
            MapSettings.NO_SNEAK, "gui.create_maps.map_settings_tab.gameplay.no_sneak",
            MapSettings.RESET_IN_WATER, "gui.create_maps.map_settings_tab.gameplay.reset_water",
            MapSettings.RESET_IN_LAVA, "gui.create_maps.map_settings_tab.gameplay.reset_lava",
            MapSettings.NO_TURN, "gui.create_maps.map_settings_tab.gameplay.no_turn"
    );
    static final Map<MapSetting<Boolean>, String> SETTINGS_ICONS = Map.of(
            MapSettings.ONLY_SPRINT, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_onlysprint_icon").model(),
            MapSettings.NO_SPRINT, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_nosprint_icon").model(),
            MapSettings.NO_JUMP, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_nojump_icon").model(),
            MapSettings.NO_SNEAK, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_nosneak_icon").model(),
            MapSettings.RESET_IN_WATER, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_reset_water_icon").model(),
            MapSettings.RESET_IN_LAVA, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_reset_lava_icon").model(),
            MapSettings.NO_TURN, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_noturning_icon").model()
    );

    private static final Sprite SPRITE = new Sprite("action/icon/setting_add", 2, 3);

    public static final Key KEY = Key.key("mapmaker:enable_setting");
    public static final StructCodec<EnableSettingAction> CODEC = StructCodec.struct(
            "setting", MapSetting.CODEC.optional(), EnableSettingAction::setting,
            EnableSettingAction::new);
    public static final Action.Editor<EnableSettingAction> EDITOR = new Action.Editor<>(
            EditorPanel::new, SPRITE, EnableSettingAction::makeThumbnail);

    public EnableSettingAction withSetting(@Nullable MapSetting<?> setting) {
        return new EnableSettingAction(setting);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        var settings = state.get(Attachments.SETTINGS);
        if (settings == null) settings = new SavedMapSettings();
        if (setting == null || !(setting.defaultValue() instanceof Boolean)) return;
        state.set(Attachments.SETTINGS, settings.with((MapSetting<Boolean>) setting, true));
    }

    private static TranslatableComponent makeThumbnail(@Nullable EnableSettingAction action) {
        if (action == null || action.setting == null)
            return translatable("gui.action.enable_setting.thumbnail.empty");
        return translatable("gui.action.enable_setting.thumbnail", List.of(
                translatable(SETTINGS_TRANSLATION_KEYS.get(action.setting) + ".name")
        ));
    }

    private static class EditorPanel extends AbstractActionEditorPanel<EnableSettingAction> {
        public EditorPanel(ActionList.Ref ref) {
            super(ref);

            background("action/editor/list_container", -10, -31);
            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose setting"));

            Consumer<MapSetting<?>> innerUpdateFunc = update(EnableSettingAction::withSetting);
            Consumer<MapSetting<?>> updateFunc = setting -> {
                innerUpdateFunc.accept(setting);
                host.popView();
            };

            for (int i = 0; i < BOOL_SETTINGS.size(); i++) {
                var setting = BOOL_SETTINGS.get(i);
                int x = i % 7, y = i / 7;

                add(x + 1, y + 2, new Button(SETTINGS_TRANSLATION_KEYS.get(setting), 1, 1)
                        .model(SETTINGS_ICONS.get(setting), null)
                        .onLeftClick(() -> updateFunc.accept(setting)));
            }
        }

        @Override
        protected void update(EnableSettingAction data) {
            // Noop, we just pop the view when updating
        }
    }

}
