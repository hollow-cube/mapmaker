package net.hollowcube.command.example;

import net.hollowcube.command.Command;

public class ParentCommand extends Command {

    public ParentCommand() {
        super("parent");

        addSubcommand(new ListCommand());
    }

}
