package net.hollowcube.terraform.legacy.compat.worldedit.command;

import net.hollowcube.terraform.legacy.compat.worldedit.util.WECommand;

public final class ChunkCommands {

    public static class ChunkInfo extends WECommand {
        public ChunkInfo() {
            super("/chunkinfo");
        }
    }

    public static class ListChunks extends WECommand {
        public ListChunks() {
            super("/listchunks");
        }
    }

    public static class DelChunks extends WECommand {
        public DelChunks() {
            super("/delchunks");
        }
    }

    private ChunkCommands() {
    }
}
