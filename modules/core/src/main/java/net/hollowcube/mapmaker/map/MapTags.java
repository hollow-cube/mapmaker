package net.hollowcube.mapmaker.map;

import net.kyori.adventure.text.Component;

public final class MapTags {
    public enum TagType {
        GAMEPLAY,
        VISUAL
    }

    public enum Tag {
        // WARNING! Changing the variable names or order of these tags is dangerous.
        // This enum must match the order of the tag declarations in the GUI xml file.
        // See the warning in EditMap for more details.

        // Visual
        TERRAIN(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.terrain.name"),
        ORGANICS(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.organics.name"),
        STRUCTURE(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.structure.name"),
        INTERIOR(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.interior.name"),
        MUSIC(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.music.name"),
        TWODIMENSIONAL(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.2D.name"),
        RECREATION(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.recreation.name"),
        STORY(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.story.name"),

        // Gameplay
        COOP(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.coop.name"),
        PUZZLE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.puzzle.name"),
        MINIGAME(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.minigame.name"),
        EXPLORATION(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.exploration.name"),
        BOSSBATTLE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.bossbattle.name"),
        AUTOCOMPLETE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.auto-complete.name"),
        ESCAPE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.escape.name"),
        TRIVIA(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.trivia.name"),
        STRATEGY(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.strategy.name"),

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

        public TagType getType() {
            return this.type;
        }
    }

}