package net.hollowcube.mapmaker.map;

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
        STORY(TagType.VISUAL, "Story", false, "book_open"),

        // Gameplay
        COOP(TagType.GAMEPLAY, "Co-Op", true),
        PUZZLE(TagType.GAMEPLAY, "Puzzle", false, "puzzle_piece"),
        MINIGAME(TagType.GAMEPLAY, "Minigame", true),
        EXPLORATION(TagType.GAMEPLAY, "Exploration", false),
        BOSSBATTLE(TagType.GAMEPLAY, "Boss Battle", false),
        AUTOCOMPLETE(TagType.GAMEPLAY, "Auto-Complete", false),
        ESCAPE(TagType.GAMEPLAY, "Escape", false),
        TRIVIA(TagType.GAMEPLAY, "Trivia", false, "brain"),
        STRATEGY(TagType.GAMEPLAY, "Strategy", false),

        // New tags, never used in EditMap
        SPEEDRUN("steve_move_right"),
        SECTIONED("flag"),
        RANKUP("flag_x"),
        GAUNTLET("fist"),
        DROPPER("steve_move_down"),
        ONE_JUMP("target"),
        TUTORIAL("graduation_hat"),
        // PUZZLE
        // TRIVIA
        TIMED("stopwatch"),
        // STORY

        ONLY_SPRINT("boot_move_right"),
        NO_SPRINT("boot_slime"),
        NO_SNEAK("steve_crouch_x"),
        NO_JUMP("arrow_up_x"),
        NO_TURNING("mouse_x"),

        BLOCK_PLACING("grass_block"),
        ELYTRA("elytra"),
        TRIDENT("trident"),
        MACE("mace"),
        SPEAR("spear"),
        ENDER_PEARL("ender_pearl"),
        WIND_CHARGE("wind_charge"),

        // ADD YOUR NEW TAGS TO THE END OF THIS ENUM
        ;

        TagType type;
        String name;
        boolean disabled;

        String sprite;

        Tag(TagType type, String name, boolean disabled) {
            this.type = type;
            this.name = name;
            this.disabled = disabled;
            this.sprite = null;
        }

        Tag(TagType type, String name, boolean disabled, String sprite) {
            this.type = type;
            this.name = name;
            this.disabled = disabled;
            this.sprite = sprite;
        }

        Tag(String sprite) {
            this.type = null;
            this.name = "";
            this.disabled = true;
            this.sprite = sprite;
        }

        public String displayName() {
            return this.name;
        }

        public TagType getType() {
            return this.type;
        }

        public boolean isDisabled() {
            return this.disabled;
        }

        public String sprite() {
            return this.sprite;
        }
    }

}