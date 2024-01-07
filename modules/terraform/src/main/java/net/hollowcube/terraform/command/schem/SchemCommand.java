package net.hollowcube.terraform.command.schem;

import net.hollowcube.command.dsl.CommandDsl;

public class SchemCommand extends CommandDsl {

    public final SchemListCommand list;
    public final SchemSaveCommand save;
    public final SchemLoadCommand load;

    public final SchemDownloadCommand download;
    public final SchemUploadCommand upload;

    public SchemCommand() {
        super("schem", "schematic");

        addSubcommand(this.list = new SchemListCommand());
        addSubcommand(this.save = new SchemSaveCommand());
        addSubcommand(this.load = new SchemLoadCommand());

        addSubcommand(this.download = new SchemDownloadCommand());
        addSubcommand(this.upload = new SchemUploadCommand());
    }

}
