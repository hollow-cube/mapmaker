package net.hollowcube.command.util;

import org.jetbrains.annotations.Nullable;

public record CommandCategory(int order, String displayName) {
    public static final @Nullable CommandCategory HIDDEN = null;
    public static final CommandCategory DEFAULT = new CommandCategory(Integer.MAX_VALUE, "ᴍɪѕᴄ");
}
