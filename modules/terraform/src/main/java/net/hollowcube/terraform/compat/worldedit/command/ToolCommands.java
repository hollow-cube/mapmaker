package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.compat.worldedit.util.WECommand;

public final class ToolCommands {

    public static class Tool extends WECommand {
        public Tool() {
            super("/tool");
        }
    }

    public static class Mask extends WECommand {
        public Mask() {
            super("/mask");
        }
    }

    public static class Material extends WECommand {
        public Material() {
            super("/material");
        }
    }

    public static class Range extends WECommand {
        public Range() {
            super("/range");
        }
    }

    public static class TraceMask extends WECommand {
        public TraceMask() {
            super("/tracemask");
        }
    }

    private ToolCommands() {
    }
}
