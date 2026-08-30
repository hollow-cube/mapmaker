package net.hollowcube.mapmaker.chat;

import net.hollowcube.ipc.chat.ChatChannel;

/// The one place the chat channel a player has selected is spelled.
///
/// It is persisted player data, so the stored values stay the strings they have always been; every
/// other use of a channel is the [ChatChannel] on the wire.
public final class ChatChannels {

    public static final String GLOBAL = "global";
    public static final String LOCAL = "local";
    public static final String STAFF = "staff";

    /// The channel a stored setting names, and global for anything it does not.
    public static ChatChannel of(String setting) {
        return switch (setting) {
            case LOCAL -> ChatChannel.LOCAL;
            case STAFF -> ChatChannel.STAFF;
            default -> ChatChannel.GLOBAL;
        };
    }

    private ChatChannels() {
    }
}
