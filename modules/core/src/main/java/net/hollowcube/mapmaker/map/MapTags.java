package net.hollowcube.mapmaker.map;

public final class MapTags {
    public enum TagType {
        GAMEPLAY,
        VISUAL
    }

    public enum Tag {
        COOP(TagType.GAMEPLAY),
        PUZZLE(TagType.GAMEPLAY),
        ESCAPE(TagType.GAMEPLAY),
        MINIGAME(TagType.GAMEPLAY),
        TRIVIA(TagType.GAMEPLAY),
        BOSSBATTLE(TagType.GAMEPLAY),
        EXPLORATION(TagType.GAMEPLAY),
        AUTOCOMPLETE(TagType.GAMEPLAY),
        STRATEGY(TagType.GAMEPLAY),
        RECREATION(TagType.VISUAL),
        TERRAIN(TagType.VISUAL),
        ORGANICS(TagType.VISUAL),
        STRUCTURE(TagType.VISUAL),
        INTERIOR(TagType.VISUAL),
        TWODIMENSIONAL(TagType.VISUAL),
        STORY(TagType.VISUAL),
        ;

        TagType type;

        Tag(TagType type) {
            this.type = type;
        }
    }

}