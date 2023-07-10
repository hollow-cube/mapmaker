package net.hollowcube.map.command;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;

public class SethJumpscareCommand extends Command {

//    private static final UUID SETH_UUID = UUID.fromString("a3634428-40a0-45b3-8583-a3b5813d64c5");
    private static final UUID SETH_UUID = UUID.fromString("7bd5b459-1e6b-4753-8274-1fbd2fe9a4d5");

    private static final Sound SOUND = Sound.sound(Key.key("staff.seth.jumpscare"), Sound.Source.MASTER, 1f, 1f);
    private static final Title TITLE = Title.title(
            Component.text("\uE010"),
            Component.empty(),
            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(2))
    );

    public SethJumpscareCommand() {
        super("sethjumpscare");

        setCondition(this::handleCondition);

        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /sethjumpscare <player>"));
        addSyntax(this::jumpscare, ArgumentType.Entity("player").onlyPlayers(true));
    }

    private void jumpscare(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("command.generic.player_only"));
            return;
        }

        EntityFinder entityFinder = context.get("player");
        for (Entity entity : entityFinder.find(sender)) {
            if (!(entity instanceof Player target)) continue;

            player.sendMessage(Component.text("You just jumpscared " + target.getUsername() + "!!"));

            target.playSound(SOUND, Sound.Emitter.self());
            target.showTitle(TITLE);
        }
    }

    private boolean handleCondition(@NotNull CommandSender sender, @Nullable String commandString) {
        if (!(sender instanceof Player player)) return false;
        return player.getUuid().equals(SETH_UUID);
    }
}
