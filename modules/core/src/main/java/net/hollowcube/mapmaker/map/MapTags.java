package net.hollowcube.mapmaker.map;

public final class MapTags {
    public enum TagType {
        VISUAL,
        GAMEPLAY
    }

    public enum Tag {
        TWODIMENSIONAL(TagType.VISUAL),
        AUTOCOMPLETE(TagType.GAMEPLAY),
        ;

        TagType type;

        Tag(TagType type) {
            this.type = type;
        }
    }

}