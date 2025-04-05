package net.hollowcube.mapmaker.command.store;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.gui.store.StoreModule;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class StoreCommand extends CommandDsl {
    private final Supplier<ScriptEngine> scriptEngine;
    private final PlayerService playerService;
    private final PermManager permManager;

    public StoreCommand(@NotNull Supplier<ScriptEngine> scriptEngine, @NotNull PlayerService playerService, @NotNull PermManager permManager) {
        super("store", "buy");
        this.scriptEngine = scriptEngine;
        this.playerService = playerService;
        this.permManager = permManager;

        category = CommandCategories.GLOBAL;
        description = "Opens our in-game store";

        addSyntax(playerOnly(this::handleOpenStore));
    }

    private void handleOpenStore(@NotNull Player player, @NotNull CommandContext context) {
        StoreModule.openStoreView(scriptEngine.get(), playerService, permManager, player, null);
    }
}
