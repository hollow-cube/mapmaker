package net.hollowcube.mapmaker.map.command.utility;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.LightingChunk;
import org.jetbrains.annotations.NotNull;

public class RelightCommand extends CommandDsl {

    @Inject
    public RelightCommand(@NotNull PermManager permManager) {
        super("relight");

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
        addSyntax(playerOnly(this::handleRelightWorld));
    }

    private void handleRelightWorld(@NotNull Player player, @NotNull CommandContext context) {
        LightingChunk.relight(player.getInstance(), player.getInstance().getChunks());
        player.sendMessage("Relighting world...");
    }
}
