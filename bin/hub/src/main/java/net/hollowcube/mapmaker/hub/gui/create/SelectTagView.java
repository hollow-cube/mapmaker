package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapTags;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;

import java.util.List;
import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class SelectTagView extends Panel {
    private static final List<MapTags.Tag> GAMEPLAY_TAGS = List.of(
        MapTags.Tag.SPEEDRUN, MapTags.Tag.SECTIONED, MapTags.Tag.RANKUP,
        MapTags.Tag.GAUNTLET, MapTags.Tag.DROPPER, MapTags.Tag.ONE_JUMP,
        MapTags.Tag.TUTORIAL, MapTags.Tag.PUZZLE, MapTags.Tag.TRIVIA,
        MapTags.Tag.TIMED, MapTags.Tag.STORY);
    private static final List<MapTags.Tag> ITEM_TAGS = List.of(
        MapTags.Tag.BLOCK_PLACING, MapTags.Tag.ELYTRA, MapTags.Tag.TRIDENT,
        MapTags.Tag.MACE, MapTags.Tag.SPEAR, MapTags.Tag.ENDER_PEARL,
        MapTags.Tag.WIND_CHARGE);
    private static final List<MapTags.Tag> SETTINGS_TAGS = List.of(
        MapTags.Tag.ONLY_SPRINT, MapTags.Tag.NO_SPRINT, MapTags.Tag.NO_SNEAK,
        MapTags.Tag.NO_JUMP, MapTags.Tag.NO_TURNING);

    private final Consumer<MapTags.Tag> onSelect;

    public SelectTagView(MapData map, Consumer<MapTags.Tag> onSelect) {
        super(9, 10);
        this.onSelect = onSelect;

        background("create_maps2/edit/tags_container", -10, -31);
        add(0, 0, title("Choose Tag"));

        add(0, 0, backOrClose());

        appendTagList(map, GAMEPLAY_TAGS, 2);
        appendTagList(map, ITEM_TAGS, 5);
        appendTagList(map, SETTINGS_TAGS, 6);
    }

    private void appendTagList(MapData map, List<MapTags.Tag> tags, int y) {
        int index = 0;
        for (var tag : tags) {
            if (map.settings().hasTag(tag))
                continue;

            var button = new Button(tag.name().toLowerCase(), 1, 1)
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
