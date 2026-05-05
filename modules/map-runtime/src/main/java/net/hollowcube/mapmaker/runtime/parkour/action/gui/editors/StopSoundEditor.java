package net.hollowcube.mapmaker.runtime.parkour.action.gui.editors;

import net.hollowcube.mapmaker.panels.AnvilSearchView;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.StopSoundAction;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.minestom.server.sound.BuiltinSoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class StopSoundEditor {

    public static @NotNull AnvilSearchView<BuiltinSoundEvent> open(@NotNull ActionList.Ref ref) {
        return AnvilSearchView.simple(
            "action/anvil/search_icon", "Search Sounds",
            (query, limit) -> {
                var search = Autocompletors.searchSounds(query, limit);
                if (query.isBlank() || query.equals("s")) {
                    var output = new ArrayList<BuiltinSoundEvent>(search.size() + 1);
                    output.add(null);
                    output.addAll(search);
                    return output;
                }
                return search;
            },
            PlaySoundEditor::makeSoundButton,
            event -> {
                ref.<StopSoundAction>update(action -> action.withEvent(event));
            }
        );
    }

    public static TranslatableComponent makeThumbnail(@Nullable StopSoundAction action) {
        if (action == null || action.event() == null)
            return Component.translatable("gui.action.stop_sound.thumbnail.empty");
        return Component.translatable("gui.action.stop_sound.thumbnail", List.of(
            Component.text(action.event().key().asMinimalString())
        ));
    }
}
