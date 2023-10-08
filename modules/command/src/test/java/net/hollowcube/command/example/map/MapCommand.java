package net.hollowcube.command.example.map;

import net.hollowcube.command.Command;

public class MapCommand extends Command {

    public MapCommand() {
        super("map");

        addSubcommand(new MapListCommand());
        addSubcommand(new MapAlterCommand());
    }

}
