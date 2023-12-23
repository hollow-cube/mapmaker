package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.Command;

public class ClipboardCommand extends Command {

    public ClipboardCommand() {
        super("c", "clipboard"); //todo aliases

        addSubcommand(new ClipClearCommand());
        addSubcommand(new ClipListCommand());
        addSubcommand(new ClipRotateCommand());
        addSubcommand(new ClipFlipCommand());
    }

}
