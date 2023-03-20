package net.hollowcube.map.command;

import net.hollowcube.mapmaker.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.number.ArgumentInteger;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MultiBuildCommand extends BaseMapCommand {
    public MultiBuildCommand() {
        super(true, "multibuild", "mb");
        setDefaultExecutor((sender, context) -> sender.sendMessage("Usage: /multibuild <1-10>"));
        var amountArg = new ArgumentInteger("1-10").min(1).max(10);
        amountArg.setCallback((sender, exception) -> {
            sender.sendMessage("Number not in range! Usage: /multibuild <1-10>");
            // todo handle invalid input
        });
        addSyntax(this::execute, amountArg);
    }

    private void execute(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.translatable("command.generic.player_only"));
            return;
        }
        PlayerData data = PlayerData.fromPlayer(player);
        Integer multibuild = context.get("1-10");
        if (multibuild == 0 || multibuild == 1) {
            data.setMultibuild(0);
            sender.sendMessage("MultiBuild disabled!");
        } else {
            if (multibuild < 2 || multibuild > 10) {
                return;
            }

            data.setMultibuild(multibuild);
            sender.sendMessage("MultiBuild set to: " + multibuild);
        }
    }
}