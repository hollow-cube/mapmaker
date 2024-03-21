package net.hollowcube.command.util;

import org.jetbrains.annotations.NotNull;

public record CommandCategory(int order, @NotNull String displayName) {
    public static final CommandCategory HIDDEN = null;
    public static final CommandCategory DEFAULT = new CommandCategory(Integer.MAX_VALUE, "ᴍɪѕᴄ");
}
