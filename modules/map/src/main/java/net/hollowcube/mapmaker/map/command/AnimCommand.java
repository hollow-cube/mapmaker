package net.hollowcube.map.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.map.MapFeatureFlags;
import net.hollowcube.map.command.animation.AnimCreateCommand;
import net.hollowcube.map.world.EditingMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.command.CommandCondition.and;
import static net.hollowcube.map.util.MapCondition.mapFeature;
import static net.hollowcube.map.util.MapCondition.mapFilter;

public class AnimCommand extends CommandDsl {
    private final Argument<Integer> tickArg = Argument.Int("tick");


    public final AnimCreateCommand create;

    public AnimCommand() {
        super("anim");

        setCondition(and(
                mapFilter(false, true, false),
                mapFeature(MapFeatureFlags.ANIMATION_BUILDER)
        ));

        addSubcommand(this.create = new AnimCreateCommand());

        addSyntax(playerOnly(this::handleSeek), Argument.Literal("seek"), tickArg);
        addSyntax(playerOnly(this::handlePlayAnimation), Argument.Literal("play"));
        addSyntax(playerOnly(this::handlePauseAnimation), Argument.Literal("pause"));
        addSyntax(playerOnly(this::handleStepAnimation), Argument.Literal("step"));

        addSyntax(playerOnly(this::handleSendDebugInfo), Argument.Literal("debug"));
    }

    private void handleSeek(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld editingWorld)) return;
        var builder = editingWorld.animationBuilder();

        var tick = context.get(tickArg);
        builder.seek(tick);
    }

    private void handlePlayAnimation(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld editingWorld)) return;
        var builder = editingWorld.animationBuilder();

        builder.play();
    }

    private void handlePauseAnimation(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld editingWorld)) return;
        var builder = editingWorld.animationBuilder();

        builder.pause();
    }

    private void handleStepAnimation(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld editingWorld)) return;
        var builder = editingWorld.animationBuilder();

        builder.step();
    }

    private void handleSendDebugInfo(@NotNull Player player, @NotNull CommandContext context) {
        var world = MapWorld.forPlayer(player);
        if (!(world instanceof EditingMapWorld editingWorld)) return;
        var builder = editingWorld.animationBuilder();

        builder.sendDebugInfo(player);
    }
}
