package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapTags;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.kyori.adventure.text.Component;

import java.util.List;

public class EditableMapTagList extends Panel {
    private final MapData map;

    public EditableMapTagList(MapData map) {
        super(7, 1);
        this.map = map;

        update();
    }

    static List<Component> getTagTranslationArgs(MapTags.Tag tag) {
        return List.of(
            Component.translatable(getFullTagTranslation(tag) + ".name"),
            Component.translatable(getFullTagTranslation(tag) + ".lore")
        );
    }

    private static String getFullTagTranslation(MapTags.Tag tag) {
        return "gui.create_maps.tags." + tag.type().translationName() + '.' + tag.translationName();
    }

    private void update() {
        clear();

        var tags = map.settings().getTags();
        int i = 0;
        for (; i < 7 && i < tags.size(); i++) {
            var tag = tags.get(i);
            final int index = i;
            add(i, 0, new Button(1, 1)
                .translationKey("gui.create_maps.tags.with_data", getTagTranslationArgs(tag))
                .sprite("icon2/1_1/" + tag.sprite(), 1, 1)
                .onLeftClick(() -> host.pushTransientView(new SelectTagView(map,
                    newTag -> handleReplaceTag(index, newTag))))
                .onRightClick(() -> handleRemoveTag(index)));
        }

        if (i < 7) {
            final String tagCategory;
            if (i <= 3) {
                tagCategory = "primary";
            } else {
                tagCategory = "secondary";
            }

            add(i, 0, new Button("gui.create_maps.edit.tags." + tagCategory + ".add", 1, 1)
                .sprite("icon2/1_1/plus", 1, 1)
                .onLeftClick(() -> host.pushTransientView(new SelectTagView(map, this::handleAddTag))));
        }
    }

    private void handleAddTag(MapTags.Tag tag) {
        map.settings().addTag(tag);
        update();
    }

    private void handleReplaceTag(int index, MapTags.Tag tag) {
        if (index >= map.settings().getTags().size()) {
            handleAddTag(tag);
            return;
        }

        map.settings().setTag(index, tag);
        update();
    }

    private void handleRemoveTag(int index) {
        map.settings().removeTag(index);
        update();
    }
}
