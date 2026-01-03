package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapTags;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;

public class EditableMapTagList extends Panel {
    private final MapData map;

    public EditableMapTagList(MapData map) {
        super(7, 1);
        this.map = map;

        update();
    }

    private void update() {
        clear();

        var tags = map.settings().getTags();
        int i = 0;
        for (; i < 7 && i < tags.size(); i++) {
            var tag = tags.get(i);
            add(i, 0, new Button("tag", 1, 1)
                .sprite("icon2/1_1/" + tag.sprite(), 1, 1));
        }

        if (i < 7) {
            add(i, 0, new Button("add_tag", 1, 1)
                .sprite("icon2/1_1/plus", 1, 1)
                .onLeftClick(() -> host.pushTransientView(new SelectTagView(map, this::handleAddTag))));
        }
    }

    private void handleAddTag(MapTags.Tag tag) {
        map.settings().addTag(tag);
        update();
    }
}
