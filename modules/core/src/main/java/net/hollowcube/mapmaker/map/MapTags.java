package net.hollowcube.mapmaker.map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;

public final class MapTags {
    public enum TagType {
        GAMEPLAY,
        VISUAL
    }

    public enum Tag {
        // WARNING! Changing the variable names or order of these tags is dangerous.
        // To support backward compatibility, consider leaving them here and deprecating
        // tags individually and removing the ability to select them in the GUI.
        // It is safe to change the display name of tags, however.
        COOP(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.coop.name"),
        PUZZLE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.puzzle.name"),
        ESCAPE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.escape.name"),
        MINIGAME(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.minigame.name"),
        TRIVIA(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.trivia.name"),
        BOSSBATTLE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.bossbattle.name"),
        EXPLORATION(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.exploration.name"),
        AUTOCOMPLETE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.auto-complete.name"),
        STRATEGY(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.strategy.name"),
        RECREATION(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.recreation.name"),
        TERRAIN(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.terrain.name"),
        ORGANICS(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.organics.name"),
        STRUCTURE(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.structure.name"),
        INTERIOR(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.interior.name"),
        TWODIMENSIONAL(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.2D.name"),
        MUSIC(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.music.name"),
        STORY(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.story.name"),
        // ADD YOUR NEW TAGS TO THE END OF THIS ENUM
        ;

        TagType type;
        String translateKey;

        Tag(TagType type, String translateKey) {
            this.type = type;
            this.translateKey = translateKey;
        }

        public Component displayName() {
            return Component.translatable(this.translateKey);
        }
    }

}