package net.hollowcube.terraform.compat.worldedit.command;

import net.hollowcube.terraform.compat.worldedit.util.WECommand;

public final class BiomeCommands {

    public static class BiomeList extends WECommand {
        public BiomeList() {
            super("/biomelist", "/biomels");
        }
    }

    public static class BiomeInfo extends WECommand {
        public BiomeInfo() {
            super("/biomeinfo");
        }
    }

    public static class SetBiome extends WECommand {
        public SetBiome() {
            super("/setbiome");
        }
    }

    private BiomeCommands() {
    }
}
