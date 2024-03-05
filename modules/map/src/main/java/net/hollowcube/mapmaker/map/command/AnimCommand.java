package net.hollowcube.mapmaker.map.command;

import net.hollowcube.command.dsl.CommandDsl;

public class AnimCommand extends CommandDsl {
//    private final Argument<Integer> tickArg = Argument.Int("tick");
//
//    public final AnimCreateCommand create;

    public AnimCommand() {
        super("anim");

//        setCondition(and(
//                mapFilter(false, true, false),
//                mapFeature(MapFeatureFlags.ANIMATION_BUILDER)
//        ));
//
//        addSubcommand(this.create = new AnimCreateCommand());
//
//        addSyntax(playerOnly(this::handleSeek), Argument.Literal("seek"), tickArg);
//        addSyntax(playerOnly(this::handlePlayAnimation), Argument.Literal("play"));
//        addSyntax(playerOnly(this::handlePauseAnimation), Argument.Literal("pause"));
//        addSyntax(playerOnly(this::handleStepAnimation), Argument.Literal("step"));
//
//        addSyntax(playerOnly(this::handleSendDebugInfo), Argument.Literal("debug"));
    }
//
//    private void handleSeek(@NotNull Player player, @NotNull CommandContext context) {
//        var world = MapWorld.forPlayer(player);
//        if (!(world instanceof EditingMapWorld editingWorld)) return;
//        var builder = editingWorld.animationBuilder();
//
//        var tick = context.get(tickArg);
//        builder.seek(tick, true);
//    }
//
//    private void handlePlayAnimation(@NotNull Player player, @NotNull CommandContext context) {
//        var world = MapWorld.forPlayer(player);
//        if (!(world instanceof EditingMapWorld editingWorld)) return;
//        var builder = editingWorld.animationBuilder();
//
//        builder.play();
//    }
//
//    private void handlePauseAnimation(@NotNull Player player, @NotNull CommandContext context) {
//        var world = MapWorld.forPlayer(player);
//        if (!(world instanceof EditingMapWorld editingWorld)) return;
//        var builder = editingWorld.animationBuilder();
//
//        builder.pause();
//    }
//
//    private void handleStepAnimation(@NotNull Player player, @NotNull CommandContext context) {
//        var world = MapWorld.forPlayer(player);
//        if (!(world instanceof EditingMapWorld editingWorld)) return;
//        var builder = editingWorld.animationBuilder();
//
//        builder.step();
//    }
//
//    private void handleSendDebugInfo(@NotNull Player player, @NotNull CommandContext context) {
//        var world = MapWorld.forPlayer(player);
//        if (!(world instanceof EditingMapWorld editingWorld)) return;
//        var builder = editingWorld.animationBuilder();
//
//        builder.sendDebugInfo(player);
//    }
}
