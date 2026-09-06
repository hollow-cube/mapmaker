package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.ipc.map.MapPatch;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.MapTags;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.LORE_POSTFIX_CLICKCHANGEORREMOVE;

public class EditableMapTagList extends Panel {
    private final MapPatch.Builder editor;
    private final Runnable onUpdate;

    public EditableMapTagList(MapPatch.Builder editor, Runnable onUpdate) {
        super(7, 1);
        this.editor = editor;
        this.onUpdate = onUpdate;

        update();
    }

    private static String getCategory(int index) {
        if (index == 0) return "primary.first";
        if (index == 1) return "primary.second";
        if (index == 2) return "primary.third";
        return "secondary";
    }

    private void update() {
        clear();

        var tags = MapSettings.getTags(editor.map().settings());
        int i = 0;
        for (; i < 7 && i < tags.size(); i++) {
            var tag = tags.get(i);
            final int index = i;
            add(i, 0, new Button(1, 1)
                .translationKey(tag.baseTranslationKey())
                .lorePostfix(LORE_POSTFIX_CLICKCHANGEORREMOVE)
                .sprite("icon2/1_1/" + tag.sprite(), 1, 1)
                .onLeftClick(() -> host.pushTransientView(new SelectTagView(editor.map(), newTag -> handleReplaceTag(index, newTag))))
                .onRightClick(() -> handleRemoveTag(index)));
        }

        if (i < 7) {
            final String tagCategory = getCategory(i);

            add(i, 0, new Button(1, 1)
                .translationKey("gui.create_maps.edit.tags.add." + tagCategory)
                .sprite("icon2/1_1/plus", 1, 1)
                .onLeftClick(() -> host.pushTransientView(new SelectTagView(editor.map(), this::handleAddTag))));
        }
    }

    private void handleAddTag(MapTags.Tag tag) {
        MapSettings.addTag(editor, tag);
        update();
        this.onUpdate.run();
    }

    private void handleReplaceTag(int index, MapTags.Tag tag) {
        if (index >= MapSettings.getTags(editor.map().settings()).size()) {
            handleAddTag(tag);
            return;
        }

        MapSettings.setTag(editor, index, tag);
        update();
        this.onUpdate.run();
    }

    private void handleRemoveTag(int index) {
        MapSettings.removeTag(editor, index);
        update();
        this.onUpdate.run();
    }
}