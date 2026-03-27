package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapTags;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;

import java.util.List;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class SelectTagView extends Panel {
    private static final List<MapTags.Tag> GAMEPLAY_TAGS = MapTags.allOfType(MapTags.TagType.GAMEPLAY);
    private static final List<MapTags.Tag> ITEM_TAGS = MapTags.allOfType(MapTags.TagType.ITEM);
    private static final List<MapTags.Tag> SETTINGS_TAGS = MapTags.allOfType(MapTags.TagType.SETTING);

    private final Consumer<MapTags.Tag> onSelect;

    public SelectTagView(MapData map, Consumer<MapTags.Tag> onSelect) {
        super(9, 10);
        this.onSelect = onSelect;

        background("create_maps2/edit/tags_container", -10, -31);
        add(0, 0, title("Choose Tag"));

        add(0, 0, backOrClose());

        add(1, 1, infoText(7, "gameplay"));
        appendTagList(map, GAMEPLAY_TAGS, 2);
        add(1, 4, infoText(7, "items"));
        appendTagList(map, ITEM_TAGS, 5);
        add(1, 6, new Text(null, 7, 0, "settings")
            .font("small").align(1, -11));
        appendTagList(map, SETTINGS_TAGS, 6);
    }

    private void appendTagList(MapData map, List<MapTags.Tag> tags, int y) {
        int index = 0;
        for (var tag : tags) {
            if (map.settings().hasTag(tag))
                continue;

            var button = new Button(1, 1)
                .translationKey("gui.create_maps.tags.select", EditableMapTagList.getTagTranslationArgs(tag))
                .sprite("icon2/1_1/" + tag.sprite(), 1, 1)
                .onLeftClick(() -> {
                    onSelect.accept(tag);
                    host.popView();
                });
            add((index % 7) + 1, (index / 7) + y, button);
            index++;
        }
    }
}