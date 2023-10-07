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
        TERRAIN(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.terrain.name", false),
        ORGANICS(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.organics.name", false),
        STRUCTURE(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.structure.name", false),
        INTERIOR(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.interior.name", false),
        MUSIC(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.music.name", true),
        TWODIMENSIONAL(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.2D.name", false),
        RECREATION(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.recreation.name", false),
        STORY(TagType.VISUAL, "gui.create_maps.map_tags_tab.visual.story.name", false),

        // Gameplay
        COOP(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.coop.name", true),
        PUZZLE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.puzzle.name", false),
        MINIGAME(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.minigame.name", true),
        EXPLORATION(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.exploration.name", false),
        BOSSBATTLE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.bossbattle.name", false),
        AUTOCOMPLETE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.auto-complete.name", false),
        ESCAPE(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.escape.name", false),
        TRIVIA(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.trivia.name", false),
        STRATEGY(TagType.GAMEPLAY, "gui.create_maps.map_tags_tab.gameplay.strategy.name", false),

        // ADD YOUR NEW TAGS TO THE END OF THIS ENUM
        ;

        TagType type;
        String translateKey;
        boolean disabled;

        Tag(TagType type, String translateKey, boolean disabled) {
            this.type = type;
            this.translateKey = translateKey;
            this.disabled = disabled;
        }

        public Component displayName() {
            return Component.translatable(this.translateKey);
        }

        public TagType getType() {
            return this.type;
        }

        public boolean isDisabled() {
            return this.disabled;
        }
    }

}