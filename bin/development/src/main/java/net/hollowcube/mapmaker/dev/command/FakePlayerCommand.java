package net.hollowcube.mapmaker.dev.command;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.fakeplayer.FakePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Consumer;

public class FakePlayerCommand extends Command {
    public FakePlayerCommand() {
        super("fakeplayer", "fp");
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /fakeplayer <name/count>"));
        addSyntax(this::parseArgument, ArgumentType.String("name/count"));
    }

    private void parseArgument(@NotNull CommandSender sender, @NotNull CommandContext context) {
        String parseArg = context.get("name/count");

        try {
            var count = Integer.parseInt(parseArg);
            for (int i = 0; i < count; i++) {
                FakePlayer.initPlayer(UUID.randomUUID(), String.valueOf(i), new Consumer<FakePlayer>() {
                    @Override
                    public void accept(FakePlayer fakePlayer) {

                    }
                });
            }
        } catch (NumberFormatException e) {
            UUID uuid = UUID.randomUUID();
            FakePlayer.initPlayer(uuid, parseArg, new Consumer<FakePlayer>() {
                @Override
                public void accept(FakePlayer fakePlayer) {

                }
            });
            sender.sendMessage("Created player with uuid " + uuid.toString());
        }
    }
}
