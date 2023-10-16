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
        TERRAIN(TagType.VISUAL, "Terrain", false),
        ORGANICS(TagType.VISUAL, "Organics", false),
        STRUCTURE(TagType.VISUAL, "Structure", false),
        INTERIOR(TagType.VISUAL, "Interior", false),
        MUSIC(TagType.VISUAL, "Music", true),
        TWODIMENSIONAL(TagType.VISUAL, "2D", false),
        RECREATION(TagType.VISUAL, "Recreation", false),
        STORY(TagType.VISUAL, "Story", false),

        // Gameplay
        COOP(TagType.GAMEPLAY, "Co-Op", true),
        PUZZLE(TagType.GAMEPLAY, "Puzzle", false),
        MINIGAME(TagType.GAMEPLAY, "Minigame", true),
        EXPLORATION(TagType.GAMEPLAY, "Exploration", false),
        BOSSBATTLE(TagType.GAMEPLAY, "Boss Battle", false),
        AUTOCOMPLETE(TagType.GAMEPLAY, "Auto-Complete", false),
        ESCAPE(TagType.GAMEPLAY, "Escape", false),
        TRIVIA(TagType.GAMEPLAY, "Trivia", false),
        STRATEGY(TagType.GAMEPLAY, "Strategy", false),

        // ADD YOUR NEW TAGS TO THE END OF THIS ENUM
        ;

        TagType type;
        String name;
        boolean disabled;

        Tag(TagType type, String name, boolean disabled) {
            this.type = type;
            this.name = name;
            this.disabled = disabled;
        }

        public String displayName() { return this.name; }

        public TagType getType() {
            return this.type;
        }

        public boolean isDisabled() {
            return this.disabled;
        }
    }

}