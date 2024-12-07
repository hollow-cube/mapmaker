package net.hollowcube.mapmaker.hub.command.util;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentEntity;
import net.hollowcube.command.arg.ArgumentWord;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class HubTrainCommand extends CommandDsl {

    private final ArgumentWord type = Argument.Word("type").with("kick", "test");
    private final ArgumentEntity players = Argument.Entity("player").onlyPlayers(true);
    private final Scheduler scheduler;

    public HubTrainCommand(@NotNull PermManager permManager, @NotNull Scheduler scheduler) {
        super("train");
        this.scheduler = scheduler;

        category = CommandCategories.STAFF;
        description = "Hits a player with a train, optionally kicking them from the server";

        setCondition(permManager.createPlatformCondition2(PlatformPerm.MAP_ADMIN));
        addSyntax(playerOnly(this::handleTrainAttack), type, players);
    }

    private void handleTrainAttack(@NotNull Player player, @NotNull CommandContext context) {
        Consumer<Player> callback = switch (context.get(type).toLowerCase(Locale.ROOT)) {
            case "kick" -> target -> target.kick(Component.text("Ran over by a train.", NamedTextColor.RED));
            default -> target -> target.sendMessage(Component.text("The train has spared you.", NamedTextColor.GRAY));
        };
        for (Entity entity : context.get(players).find(player)) {
            if (entity instanceof Player target) {
                Train train = new Train(target, callback);
                scheduler.submitTask(train::getTask);
            }
        }
    }

    private static class Train {
        private final List<NpcItemModel> train;
        private final Player target;
        private final Consumer<Player> callback;
        private int state = 0;
        private final double distanceFromPlayer = 30; // How far the train travels
        private final int windupDelay = 20; // How many ticks before the train charges the player
        private final int trainActive = 20; // How many ticks the train will move (and be visible) for
        private Vec trainDirection;

        Train(@NotNull Player target, @NotNull Consumer<Player> callback) {
            this.target = target;
            this.callback = callback;
            NpcItemModel trainFront = new NpcItemModel();
            NpcItemModel trainMiddle = new NpcItemModel();
            NpcItemModel trainBack = new NpcItemModel();
            // TODO(1.21.4)
//            trainFront.setModel(Material.STICK, 7);
//            trainMiddle.setModel(Material.STICK, 6);
//            trainBack.setModel(Material.STICK, 7);
            train = List.of(trainFront, trainMiddle, trainBack);
        }

        public TaskSchedule getTask() {
            if (state == 0) {
                for (NpcItemModel model : train) {
                    // Spawn the train in the world, we will set the proper position in the charge step
                    model.getEntityMeta().setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(), Math.toRadians(-90)).into());
                    model.getEntityMeta().setScale(new Vec(0));
                    model.setInstance(target.getInstance(), target.getPosition().add(target.getPosition().direction().withY(0).normalize().mul(20)));
                }
                target.sendMessage(Component.text("You hear the rumbling of a distant train...", NamedTextColor.RED));
                // Spawn train
                state++;
                return TaskSchedule.tick(windupDelay - 2); // Spare 2 ticks to move it into the right position
            } else if (state == 1) {
                // Correct position (if the player has been moving)
                trainDirection = target.getPosition().direction().withY(0).normalize();
                Pos trainStart = target.getPosition().add(trainDirection.mul(distanceFromPlayer));
                int offset = 0;
                for (NpcItemModel model : train) {
                    // Move further parts of the train back with offset so it doesn't overlap
                    Pos offsetStart = trainStart.add(trainDirection.mul(5 * offset)).withView(target.getPosition().yaw() + 90f, 0f);
                    model.teleport(offsetStart);
                    offset++;
                }
                state++;
                return TaskSchedule.tick(2);
            } else if (state == 2) {
                // Make train visible (set scale to 4), and make it move
                for (NpcItemModel model : train) {
                    // Move further parts of the train back with offset so it doesn't overlap
                    model.getEntityMeta().setScale(new Vec(4));
                    model.getEntityMeta().setPosRotInterpolationDuration(trainActive);
                    model.teleport(model.getPosition().add(trainDirection.mul(distanceFromPlayer * -2)));
                }
                state++;
                return TaskSchedule.tick(trainActive / 2);
            } else if (state == 3) {
                // Ram Player
                callback.accept(target);
                state++;
                return TaskSchedule.tick(trainActive / 2);
            } else if (state >= 4) {
                // End, despawn
                for (NpcItemModel model : train) {
                    model.remove();
                }
                return TaskSchedule.stop();
            }
            // Invalid state, end
            return TaskSchedule.stop();
        }
    }
}
