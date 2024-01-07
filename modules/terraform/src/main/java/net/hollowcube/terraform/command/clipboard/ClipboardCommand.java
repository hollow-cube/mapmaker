package net.hollowcube.terraform.command.clipboard;

import net.hollowcube.command.dsl.CommandDsl;

public class ClipboardCommand extends CommandDsl {

    public ClipboardCommand() {
        super("c", "clipboard");

        addSubcommand(new ClipClearCommand());
        addSubcommand(new ClipListCommand());
        addSubcommand(new ClipRotateCommand());
        addSubcommand(new ClipFlipCommand());
    }

}
