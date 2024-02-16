package net.hollowcube.map.command.build;

import com.google.inject.Inject;
import net.hollowcube.command.CommandCondition;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.worldold.EditingMapWorld;
import net.hollowcube.map.worldold.MapWorld;
import net.hollowcube.map.worldold.TestingMapWorld;
import net.hollowcube.mapmaker.map.MapVariant;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestCommand extends CommandDsl {

    @Inject
    public TestCommand() {
        super("test");

        setCondition(this::isInBuildOrTestMap);
        addSyntax(playerOnly(this::enterBuildMode));
    }

    private void enterBuildMode(@NotNull Player player, @NotNull CommandContext context) {
        var map = MapWorld.forPlayerOptional(player);
        if (map instanceof TestingMapWorld testingWorld) {
            testingWorld.exitTestMode(player);
        } else if (map.map().settings().getVariant() == MapVariant.BUILDING) {
            player.sendMessage(Component.translatable("command.test.gameplay_only"));
        } else if (map instanceof EditingMapWorld editingWorld) {
            editingWorld.enterTestMode(player);
        } else throw new IllegalStateException("unreachable");
    }

    private int isInBuildOrTestMap(@NotNull CommandSender sender, @NotNull CommandContext unused) {
        // Stupid amount of checks to verify they're actually in a world otherwise nullptr exception
        // Probably better way to structure instantiation of this command so it doesn't have this issue
        if (!(sender instanceof Player player)) {
            return CommandCondition.HIDE;
        }

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return CommandCondition.HIDE;

        return (world.flags() & (MapWorld.FLAG_TESTING | MapWorld.FLAG_EDITING)) != 0 ? CommandCondition.ALLOW : CommandCondition.HIDE;
    }
}
