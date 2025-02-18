package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.util.CommandCategory;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.util.thesneaky.TheSneaky;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.entity.EntityFinder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PlayerInfoCommand extends CommandDsl {

    private static final Tag<Set<String>> PLAYER_CHANNELS = Tag.Transient("packets:player/channels");
    private static final Tag<String> CLIENT_BRAND = Tag.Transient("packets:client/brand");

    private static final List<String> KNOWN_MODS = List.of(
            "essential",
            "lunarclient",
            "noxesium-v2",
            "axiom",
            "worldedit"
    );

    private final Argument<EntityFinder> playerArg = Argument.Entity("player")
            .onlyPlayers(true)
            .sameWorld(true);
    private final Argument<PlayerInfoType> typeArg = Argument.Enum("type", PlayerInfoType.class);

    public PlayerInfoCommand(@NotNull PermManager permManager) {
        super("playerinfo");

        category = CommandCategory.HIDDEN;

        setCondition(permManager.createPlatformCondition2(PlatformPerm.BAN_PLAYER));

        addSyntax(playerOnly(this::execute), playerArg, typeArg);
    }

    private void execute(@NotNull Player player, @NotNull CommandContext context) {
        var type = context.get(typeArg);
        var target = context.get(playerArg).findFirstPlayer(player);

        if (target == null) {
            player.sendMessage("Player with name %s not found".formatted(context.getRaw(playerArg)));
            return;
        }

        switch (type) {
            case GENERAL -> sendGeneral(player, target);
            case CHANNELS -> sendChannels(player, target, false);
            case CHANNEL_NAMESPACES -> sendChannels(player, target, true);
            case INFO_REPORTS -> sendReports(player, target);
        }
    }

    private void sendChannels(@NotNull Player player, @NotNull Player target, boolean namespaces) {
        Collection<String> channels = target.getTag(PLAYER_CHANNELS);
        if (channels == null) {
            player.sendMessage("No channels found for %s".formatted(target.getUsername()));
        } else {

            channels = channels.stream()
                    .map(s -> namespaces ? s.split(":")[0] : s)
                    .distinct()
                    .sorted()
                    .toList();
            Component message = Component.text((namespaces ? "Namespaces for (%s): " : "Channels for (%s): ").formatted(target.getUsername()))
                            .appendNewline()
                            .append(Component.text(String.join(", ", channels)));
            player.sendMessage(message);
        }
    }

    private void sendGeneral(@NotNull Player player, @NotNull Player target) {
        Set<String> channels = Objects.requireNonNullElse(target.getTag(PLAYER_CHANNELS), Set.of());
        Set<String> namespaces = channels.stream().map(s -> s.split(":")[0]).collect(Collectors.toSet());
        String brand = Objects.requireNonNullElse(target.getTag(CLIENT_BRAND), "Unknown");

        Component info = Component.empty()
                .append(Component.text("Player info for %s".formatted(target.getUsername()))).appendNewline()
                .append(Component.text("Settings: ")).append(Component.text(target.getSettings().toString()))
                .appendNewline()
                .append(Component.text("Brand: ")).append(Component.text(brand))
                .appendNewline()
                .append(Component.text("Mods:"))
                .appendNewline();

        for (String mod : KNOWN_MODS) {
            boolean present = channels.contains(mod) || namespaces.contains(mod);
            info = info.append(Component.text(" - %s: ".formatted(mod))
                    .append(trueFalse(present, "Present", "Not Present"))
                    .append(Component.newline())
            );
        }

        player.sendMessage(info.appendNewline());
    }

    private void sendReports(@NotNull Player player, @NotNull Player target) {
        TheSneaky.test(target).thenAccept(report -> {
            if (report == null) {
                player.sendMessage("No info reports found for %s".formatted(target.getUsername()));
            } else {
                Component message = Component.text("Info Reports for %s".formatted(target.getUsername()))
                        .appendNewline()
                        .append(Component.text("Mod cleared translations: ")
                                .append(trueFalse(report.aModClearedTranslations(), "Yes", "No"))
                        )
                        .appendNewline()
                        .append(Component.text("Entries:"))
                        .appendNewline();

                for (var entry : report.entries().entrySet()) {
                    var status = switch (entry.getValue()) {
                        case PRESENT -> Component.text("Present").color(NamedTextColor.RED);
                        case LIKELY_PRESENT -> Component.text("Likely Present").color(NamedTextColor.YELLOW);
                        case UNKNOWN -> Component.text("Unknown").color(NamedTextColor.GRAY);
                    };

                    message = message.append(Component.text(" - %s: ".formatted(entry.getKey()))
                            .append(status)
                            .append(Component.newline())
                    );
                }

                player.sendMessage(message);
            }
        });
    }

    private static Component trueFalse(boolean value, String trueText, String falseText) {
        return Component.text(value ? trueText : falseText).color(value ? NamedTextColor.GREEN : NamedTextColor.RED);
    }

    private enum PlayerInfoType {
        GENERAL,
        CHANNELS,
        CHANNEL_NAMESPACES,
        INFO_REPORTS,
    }

}
