package net.hollowcube.mapmaker.map;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public final class MapTags {
    public enum TagType {
        GAMEPLAY_OLD,
        GAMEPLAY,
        SETTING,
        ITEM,
        VISUAL;

        public String translationName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Tag {
        // WARNING! Changing the variable names or order of these tags is dangerous.
        // This enum must match the order of the tag declarations in the GUI xml file.
        // See the warning in EditMap for more details.

        // Visual
        TERRAIN(TagType.VISUAL, "Terrain", false, "rock_grass"),
        ORGANICS(TagType.VISUAL, "Organics", false, "plant"),
        STRUCTURE(TagType.VISUAL, "Structure", false, "house_4"),
        INTERIOR(TagType.VISUAL, "Interior", false, "couch"),
        MUSIC(TagType.VISUAL, "Music", true, "music_disc"),
        TWODIMENSIONAL(TagType.VISUAL, "2D", false, "tv_flatscreen"),
        RECREATION(TagType.VISUAL, "Recreation", false, "eiffel_tower"),
        STORY(TagType.VISUAL, "Story", false, "book_open"),

        // Gameplay
        COOP(TagType.GAMEPLAY_OLD, "Co-Op", true, "hands_shaking"),
        PUZZLE(TagType.GAMEPLAY, "Puzzle", false, "puzzle_piece"), // this makes it show first in the tag selection menu but whatever lol
        MINIGAME(TagType.GAMEPLAY_OLD, "Minigame", true, "joystick"),
        EXPLORATION(TagType.GAMEPLAY_OLD, "Exploration", false, "treasure_map"),
        BOSSBATTLE(TagType.GAMEPLAY_OLD, "Boss Battle", false, "ender_dragon_head"),
        AUTOCOMPLETE(TagType.GAMEPLAY_OLD, "Auto-Complete", false, "robot_arm"),
        ESCAPE(TagType.GAMEPLAY_OLD, "Escape", false, "door_open_prisoner"),
        TRIVIA(TagType.GAMEPLAY_OLD, "Trivia", false, "brain"),
        STRATEGY(TagType.GAMEPLAY_OLD, "Strategy", false, "rook"),

        // New tags, never used in EditMap
        SPEEDRUN(TagType.GAMEPLAY, "steve_move_right"),
        SECTIONED(TagType.GAMEPLAY, "flag"),
        RANKUP(TagType.GAMEPLAY, "flag_x"),
        GAUNTLET(TagType.GAMEPLAY, "fist"),
        DROPPER(TagType.GAMEPLAY, "steve_move_down"),
        ONE_JUMP(TagType.GAMEPLAY, "target"),
        TUTORIAL(TagType.GAMEPLAY, "graduation_hat"),
        // PUZZLE
        // TRIVIA
        TIMED(TagType.GAMEPLAY, "stopwatch"),
        // STORY

        ONLY_SPRINT(TagType.SETTING, "boot_move_right"),
        NO_SPRINT(TagType.SETTING, "boot_slime"),
        NO_SNEAK(TagType.SETTING, "steve_crouch_x"),
        NO_JUMP(TagType.SETTING, "arrow_up_x"),
        NO_TURNING(TagType.SETTING, "mouse_x"),

        BLOCK_PLACING(TagType.ITEM, "grass_block"),
        ELYTRA(TagType.ITEM, "elytra"),
        TRIDENT(TagType.ITEM, "trident"),
        // MACE(TagType.ITEM, "mace"),
        // SPEAR(TagType.ITEM, "spear"),
        ENDER_PEARL(TagType.ITEM, "ender_pearl"),
        WIND_CHARGE(TagType.ITEM, "wind_charge"),

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

        Tag(TagType type, String sprite) {
            this.type = type;
            this.name = "";
            this.disabled = true;
            this.sprite = sprite;
        }

        public String displayName() {
            return this.name;
        }

        public String translationName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public String baseTranslationKey() {
            return "gui.create_maps.tags." + type().translationName() + '.' + translationName();
        }

        public TagType type() {
            return this.type;
        }

        public boolean isDisabled() {
            return this.disabled;
        }

        public String sprite() {
            return this.sprite;
        }
    }

    public static List<Tag> allOfType(TagType type) {
        return EnumSet.allOf(Tag.class).stream().filter(tag -> tag.type == type).toList();
    }

}