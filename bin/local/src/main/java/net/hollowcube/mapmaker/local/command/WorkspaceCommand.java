package net.hollowcube.mapmaker.local.command;

import com.google.inject.Inject;
import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ArgumentMap;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.mapmaker.config.ConfigLoaderV3;
import net.hollowcube.mapmaker.local.config.LocalWorkspace;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.world.LocalEditingMapWorld;
import net.hollowcube.mapmaker.map.world.LocalTestingMapWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Stream;

public class WorkspaceCommand extends CommandDsl {
    private final Argument<String> wsNameArg = Argument.Word("name")
            .map(new ArgumentMap.ParseFunc<>() {
                @Override
                public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @UnknownNullability String raw) {
                    return new ParseResult.Success<>(raw);
                }
            }, new ArgumentMap.SuggestFunc() {
                @Override
                public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
                    getProjects(workspace.path())
                            .map(p -> p.getFileName().toString().toLowerCase(Locale.ROOT))
                            .filter(n -> n.startsWith(raw.toLowerCase(Locale.ROOT)))
                            .forEach(suggestion::add);
                }
            });

    private final LocalWorkspace workspace;
    private final ServerBridge bridge;

    @Inject
    public WorkspaceCommand(@NotNull ConfigLoaderV3 config, @NotNull ServerBridge bridge) {
        super("workspace", "ws");
        this.workspace = config.get(LocalWorkspace.class);
        this.bridge = bridge;

        addSyntax(playerOnly(this::showWorkspaceList), Argument.Literal("list"));
        addSyntax(playerOnly(this::editWorkspace), Argument.Literal("edit"), wsNameArg);
        addSyntax(playerOnly(this::showWorkspaceInfo));
    }

    private void showWorkspaceInfo(@NotNull Player player, @NotNull CommandContext context) {
        try {
            var path = workspace.path().toRealPath();
            var pathComponent = Component.text(path.getFileName().toString())
                    .hoverEvent(Component.text(path.toString()));
            var message = Component.text()
                    .append(Component.text("Workspace: ").append(pathComponent));
            var world = MapWorld.forPlayerOptional(player);
            if (world instanceof LocalEditingMapWorld editWorld) {
                message.appendNewline().append(Component.text("Project: ")
                        .append(Component.text(editWorld.map().id())));
            } else if (world instanceof LocalTestingMapWorld testWorld) {
                message.appendNewline().append(Component.text("Project: ")
                        .append(Component.text(testWorld.buildWorld().map().id())));
            }
            player.sendMessage(message);
        } catch (Exception e) {
            player.sendMessage("Failed to read workspace info: " + e.getMessage());
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    private void showWorkspaceList(@NotNull Player player, @NotNull CommandContext context) {
        try {
            var children = getProjects(workspace.path())
                    .map(p -> p.getFileName().toString())
                    .toList();

            player.sendMessage(Component.text("Projects: " + children.size()));
            for (var child : children) {
                player.sendMessage(Component.text(" - ").append(Component.text(child)
                        .hoverEvent(Component.text("Click to edit"))
                        .clickEvent(ClickEvent.runCommand("workspace edit " + child))));
            }
        } catch (Exception e) {
            player.sendMessage("Failed to read workspace info: " + e.getMessage());
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

    private void editWorkspace(@NotNull Player player, @NotNull CommandContext context) {
        bridge.joinMap(player, context.get(wsNameArg), ServerBridge.JoinMapState.EDITING, "localserver/wsjoin");
    }

    private static @NotNull Stream<Path> getProjects(@NotNull Path root) {
        try {
            return Files.list(root)
                    .filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("mmproj.json")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
