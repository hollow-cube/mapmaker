package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractMapEditor extends View {

    private @ContextObject MapService mapService;

    private @Outlet("map_name") Text mapNameText;

    private @Outlet("set_map_icon_switch") Switch setMapIconSwitch;
    private @Outlet("set_map_icon_set") Label setMapIconSetLabel;

    protected MapData map;

    protected AbstractMapEditor(@NotNull Context context) {
        super(context);
    }

    protected void updateElementsFromMap() {

        // Name
        var name = map.settings().getName();
        if (name.isEmpty()) {
            mapNameText.setText(MapData.DEFAULT_NAME, TextColor.color(0xB0B0B0)); // Light gray color
        } else {
            mapNameText.setText(name);
        }

        // Icon
        var icon = map.settings().getIcon();
        if (icon != null) {
            var translationKey = String.format(
                    "%s.%s.%s",
                    icon.isBlock() ? "block" : "item",
                    icon.key().namespace(),
                    icon.key().value()
            );
            setMapIconSetLabel.setArgs(Component.translatable(translationKey));
            setMapIconSwitch.setOption(1);
        } else {
            setMapIconSwitch.setOption(0);
        }
    }

    // MAP NAME EDITING
    // Yoinked straight from EditMap, would be nicer to make this common logic

    @Action("map_name")
    private @NonBlocking void beginUpdateMapName() {
        pushView(c -> new SetMapName(c, map.settings().getName()));
    }

    @Signal(SetMapName.SIG_UPDATE_NAME)
    private @NonBlocking void finishUpdateMapName(@NotNull String newName) {
        int maxLength = 20;
        //TODO make this only update the display of the name in the GUI, appending ... to the end, and not messing with the actual name
        String limitedName = newName.length() > maxLength ? newName.substring(0, maxLength) : newName;

        map.settings().setName(limitedName);
        updateElementsFromMap();

        //todo need to only dispatch one of these tasks at once and have some deduplication logic
        async(() -> map.settings().withUpdateRequest(req -> {
            //todo if update fails we should revert the name change and indicate to the user that it failed
            try {
                mapService.updateMap(player().getUuid().toString(), map.id(), req);
                return true;
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player());
                return false;
            }
        }));
    }

    // MAP ICON EDITING
    // Yoinked straight from EditMap, would be nicer to make this common logic

    @Action("set_map_icon_unset")
    private @NonBlocking void beginUpdateMapIcon1() {
        pushView(SetMapIcon::new);
    }

    @Action("set_map_icon_set")
    private @NonBlocking void beginUpdateMapIcon2() {
        pushView(SetMapIcon::new);
    }

    @Signal(SetMapIcon.SIG_UPDATE_ICON)
    private @NonBlocking void finishUpdateMapIcon(@NotNull Material newMaterial) {
        map.settings().setIcon(newMaterial);
        updateElementsFromMap();

        async(() -> map.settings().withUpdateRequest(req -> {
            //todo if update fails we should revert the name change and indicate to the user that it failed
            try {
                mapService.updateMap(player().getUuid().toString(), map.id(), req);
                return true;
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player());
                return false;
            }
        }));
    }
}
