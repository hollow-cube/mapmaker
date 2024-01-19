package net.hollowcube.mapmaker.cosmetic;

import org.jetbrains.annotations.NotNull;

public enum CosmeticType {
    HEAD("head");

    private final String id;

    CosmeticType(String id) {
        this.id = id;
    }

    public @NotNull String id() {
        return id;
    }
}
