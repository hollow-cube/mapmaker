package net.hollowcube.mapmaker.map.gui.effect.settings;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.feature.play.setting.SavedMapSettings;
import net.hollowcube.mapmaker.map.setting.MapSetting;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SettingsEditorView extends View {

    private static final List<MapSetting<Boolean>> SETTINGS = List.of(
            MapSettings.NO_JUMP,
            MapSettings.NO_SPRINT,
            MapSettings.ONLY_SPRINT,
            MapSettings.NO_SNEAK,
            MapSettings.RESET_IN_WATER,
            MapSettings.RESET_IN_LAVA
    );

    private @Outlet("map_settings_no_jump_switch") Switch mapSettingsNoJump;
    private @Outlet("map_settings_no_sprint_switch") Switch mapSettingsNoSprint;
    private @Outlet("map_settings_only_sprint_switch") Switch mapSettingsOnlySprint;
    private @Outlet("map_settings_no_sneak_switch") Switch mapSettingsNoSneak;
    private @Outlet("map_settings_reset_in_water_switch") Switch mapSettingsResetWater;
    private @Outlet("map_settings_reset_in_lava_switch") Switch mapSettingsResetLava;

    private final SavedMapSettings settings;

    public SettingsEditorView(@NotNull Context context, @NotNull SavedMapSettings settings, Runnable save) {
        super(context);
        this.settings = settings;

        updateState();

        for (MapSetting<Boolean> setting : SETTINGS) {

            addActionHandler(
                    "map_settings_" + setting.key(),
                    Label.ActionHandler.lmb(player -> {
                        settings.set(setting, false);
                        updateState();
                        save.run();
                    })
            );

            addActionHandler(
                    "map_settings_" + setting.key() + "_unset",
                    Label.ActionHandler.lmb(player -> {
                        settings.set(setting, true);
                        updateState();
                        save.run();
                    })
            );

            addActionHandler(
                    "map_settings_" + setting.key() + "_set",
                    Label.ActionHandler.lmb(player -> {
                        settings.reset(setting);
                        updateState();
                        save.run();
                    })
            );
        }
    }

    private void updateState() {
        mapSettingsNoJump.setOption(getSwitchValue(settings.getOrNull(MapSettings.NO_JUMP)));
        mapSettingsNoSprint.setOption(getSwitchValue(settings.getOrNull(MapSettings.NO_SPRINT)));
        mapSettingsOnlySprint.setOption(getSwitchValue(settings.getOrNull(MapSettings.ONLY_SPRINT)));
        mapSettingsNoSneak.setOption(getSwitchValue(settings.getOrNull(MapSettings.NO_SNEAK)));
        mapSettingsResetWater.setOption(getSwitchValue(settings.getOrNull(MapSettings.RESET_IN_WATER)));
        mapSettingsResetLava.setOption(getSwitchValue(settings.getOrNull(MapSettings.RESET_IN_LAVA)));
    }

    private static int getSwitchValue(Boolean bool) {
        return bool == null ? 0 : bool ? 2 : 1;
    }

}
