package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

public enum MapVariant {
    PARKOUR(2, 2),
    BUILDING(3, 0),
    ADVENTURE(0, 0);

    private final int maxVisualTags;
    private final int maxGameplayTags;

    MapVariant(int maxVisualTags, int maxGameplayTags) {
        this.maxVisualTags = maxVisualTags;
        this.maxGameplayTags = maxGameplayTags;
    }

    public int maxVisualTags() {
        return maxVisualTags;
    }

    public int maxGameplayTags() {
        return maxGameplayTags;
    }

    public int maxTags(@NotNull MapTags.TagType tagType) {
        return switch (tagType) {
            case VISUAL -> maxVisualTags();
            case GAMEPLAY -> maxGameplayTags();
        };
    }
}
