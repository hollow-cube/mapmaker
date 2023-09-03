package net.hollowcube.mapmaker.map;

public final class MapTags {
    public enum TagType {
        GAMEPLAY,
        VISUAL
    }

    public enum Tag {
        COOP(TagType.GAMEPLAY, "Co-Op"),
        PUZZLE(TagType.GAMEPLAY, "Puzzle"),
        ESCAPE(TagType.GAMEPLAY, "Escape"),
        MINIGAME(TagType.GAMEPLAY, "Minigame"),
        TRIVIA(TagType.GAMEPLAY, "Trivia"),
        BOSSBATTLE(TagType.GAMEPLAY, "Boss Battle"),
        EXPLORATION(TagType.GAMEPLAY, "Exploration"),
        AUTOCOMPLETE(TagType.GAMEPLAY, "Auto-Complete"),
        STRATEGY(TagType.GAMEPLAY, "Strategy"),
        RECREATION(TagType.VISUAL, "Recreation"),
        TERRAIN(TagType.VISUAL, "Terrain"),
        ORGANICS(TagType.VISUAL, "Organics"),
        STRUCTURE(TagType.VISUAL, "Structure"),
        INTERIOR(TagType.VISUAL, "Interior"),
        TWODIMENSIONAL(TagType.VISUAL, "2D"),
        MUSIC(TagType.VISUAL, "Music"),
        STORY(TagType.VISUAL, "Story"),
        ;

        TagType type;
        String name;

        Tag(TagType type, String name) {
            this.type = type;
            this.name = name;
        }
    }

}