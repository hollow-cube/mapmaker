package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RulesCommand extends CommandDsl {
    public RulesCommand() {
        super("rules");

        addSyntax(this::showRules);
    }

    private void showRules(@NotNull CommandSender sender, @NotNull CommandContext context) {
        sender.sendMessage(LanguageProviderV2.translateMultiMerged("command.rules", List.of()));
    }
}
