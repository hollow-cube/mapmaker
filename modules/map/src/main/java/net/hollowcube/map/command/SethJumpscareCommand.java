package net.hollowcube.map.command;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

public class SethJumpscareCommand extends Command {

    private static final UUID SETH_UUID = UUID.fromString("a3634428-40a0-45b3-8583-a3b5813d64c5");

    public SethJumpscareCommand() {
        super("sethjumpscare");

        setCondition(this::handleCondition);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /sethjumpscare <player>"));
        addSyntax(this::jumpscare, ArgumentType.Entity("player").onlyPlayers(true).singleEntity(true));
    }

    private void jumpscare(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("command.generic.player_only"));
            return;
        }

        EntityFinder entityFinder = context.get("player");
        Player target = entityFinder.findFirstPlayer(sender);

        player.sendMessage(Component.text("You just jumpscared " + target.getUsername() + "!!"));

        target.playSound(Sound.sound(Key.key("staff.seth.jumpscare"), Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self());
        target.showTitle(Title.title(
                Component.text("\uE010"),
                Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(2))
        ));
    }

    private boolean handleCondition(@NotNull CommandSender sender, @Nullable String commandString) {
        if (!(sender instanceof Player player)) return false;
        return player.getUuid() == SETH_UUID;
    }
}
