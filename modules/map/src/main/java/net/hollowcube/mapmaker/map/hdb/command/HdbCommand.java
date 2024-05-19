package net.hollowcube.mapmaker.map.hdb.command;

import com.google.inject.Inject;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.hollowcube.mapmaker.map.hdb.gui.HdbBrowserView;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.util.MapCondition.mapFilter;

public class HdbCommand extends CommandDsl {

    private final HeadDatabase hdb;
    private final Controller guiController;

    @Inject
    public HdbCommand(@NotNull HeadDatabase hdb, @NotNull Controller guiController) {
        super("headdb", "hdb");
        this.hdb = hdb;
        this.guiController = guiController;
        
        setCondition(mapFilter(false, true, false));

        addSubcommand(new HdbSearchCommand(hdb, guiController));
        addSubcommand(new HdbGiveCommand(hdb));
        addSubcommand(new HdbBase64Command(hdb));

        addSyntax(playerOnly(this::handleOpenMainGui));
    }

    private void handleOpenMainGui(@NotNull Player player, @NotNull CommandContext context) {
        this.guiController.show(player, c -> new HdbBrowserView(c));
    }

    static class ArgumentCategory extends Argument<String> {
        private final HeadDatabase hdb;

        public ArgumentCategory(@NotNull String id, @NotNull HeadDatabase hdb) {
            super(id);
            this.hdb = hdb;
        }

        @Override
        public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
            if (!hdb.isLoaded()) return syntaxError();

            //todo

            return null;
        }
    }

}
