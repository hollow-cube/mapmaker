package net.hollowcube.terraform.command.tool;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.terraform.tool.ToolHandler;
import net.hollowcube.terraform.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToolCreateCommand extends CommandDsl {
    private final Argument<String> builtinToolArg;

    private final ToolHandler toolHandler;

    public ToolCreateCommand(@NotNull ToolHandler toolHandler) {
        super("create");
        this.toolHandler = toolHandler;

        builtinToolArg = Argument.Word("type").with(toolHandler.getToolNames());

        addSyntax(playerOnly(this::createBuiltinTool), builtinToolArg);
    }

    private void createBuiltinTool(@NotNull Player player, @NotNull CommandContext context) {
        var toolType = context.get(builtinToolArg);
        var toolItem = toolHandler.createBuiltinTool(toolType);
        PlayerUtil.smartAddItemStack(player, toolItem);
        player.sendMessage(Component.translatable("tool.create", Component.translatable(toolType)));
    }


}
