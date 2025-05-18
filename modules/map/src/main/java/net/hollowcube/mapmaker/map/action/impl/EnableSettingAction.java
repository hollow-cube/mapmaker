package net.hollowcube.mapmaker.map.action.impl;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.action.AbstractAction;
import net.hollowcube.mapmaker.map.action.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.codec.StructCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Only supports boolean settings for now, should revisit later.
public class EnableSettingAction extends AbstractAction<EnableSettingAction.Data> {
    public static final EnableSettingAction INSTANCE = new EnableSettingAction();

    static final List<MapSetting<Boolean>> BOOL_SETTINGS = List.of(
            MapSettings.ONLY_SPRINT, MapSettings.NO_SPRINT,
            MapSettings.NO_JUMP, MapSettings.NO_SNEAK,
            MapSettings.RESET_IN_WATER, MapSettings.RESET_IN_LAVA);
    // These maps are pretty gross should revisit. They must match the list above
    static final Map<MapSetting<Boolean>, String> SETTINGS_TRANSLATION_KEYS = Map.of(
            MapSettings.ONLY_SPRINT, "gui.create_maps.map_settings_tab.gameplay.only_sprint",
            MapSettings.NO_SPRINT, "gui.create_maps.map_settings_tab.gameplay.no_sprint",
            MapSettings.NO_JUMP, "gui.create_maps.map_settings_tab.gameplay.no_jump",
            MapSettings.NO_SNEAK, "gui.create_maps.map_settings_tab.gameplay.no_sneak",
            MapSettings.RESET_IN_WATER, "gui.create_maps.map_settings_tab.gameplay.reset_water",
            MapSettings.RESET_IN_LAVA, "gui.create_maps.map_settings_tab.gameplay.reset_lava");
    static final Map<MapSetting<Boolean>, String> SETTINGS_ICONS = Map.of(
            MapSettings.ONLY_SPRINT, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_onlysprint_icon").model(),
            MapSettings.NO_SPRINT, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_nosprint_icon").model(),
            MapSettings.NO_JUMP, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_nojump_icon").model(),
            MapSettings.NO_SNEAK, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_nosneak_icon").model(),
            MapSettings.RESET_IN_WATER, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_reset_water_icon").model(),
            MapSettings.RESET_IN_LAVA, BadSprite.require("create_maps/tab_settings/tab_gameplay/setting_reset_lava_icon").model());

    private static final Sprite SPRITE = new Sprite("action/icon/setting_add", 2, 3);

    public record Data(@Nullable MapSetting<?> setting) {
        // Null has no effect when executed.
        public static final StructCodec<Data> CODEC = StructCodec.struct(
                "setting", MapSetting.CODEC.optional(), Data::setting,
                Data::new);

        public @NotNull Data withSetting(@Nullable MapSetting<?> setting) {
            return new Data(setting);
        }
    }

    public EnableSettingAction() {
        super("mapmaker:enable_setting", Data.CODEC, new Data(null));
    }

    @Override
    public @NotNull Sprite sprite(@Nullable Data data) {
        return SPRITE;
    }

    @Override
    public @NotNull AbstractActionEditorPanel<Data> createEditor(@NotNull ActionList.ActionData<Data> actionData) {
        return new Editor(actionData);
    }

    static class Editor extends AbstractActionEditorPanel<Data> {
        public Editor(@NotNull ActionList.ActionData<Data> actionData) {
            super(actionData);

            background("action/editor/list_container", -10, -31);
            add(1, 1, AbstractActionEditorPanel.groupText(7, "choose setting"));

            Consumer<MapSetting<?>> innerUpdateFunc = update(Data::withSetting);
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
        protected void update(@NotNull Data data) {
            // Noop, we just pop the view when updating
        }
    }

}
