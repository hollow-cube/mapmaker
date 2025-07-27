package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.compat.impl.PacketQueue;
import net.hollowcube.mapmaker.session.PlayerSession;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

class GeneralInfoType extends PlayerInfoType.ForPlayer {

    private static final Tag<String> CLIENT_BRAND = Tag.Transient("packets:client/brand");
    private static final List<String> KNOWN_MODS = List.of(
            "essential",
            "lunarclient",
            "feather",
            "noxesium-v2",
            "axiom",
            "worldedit"
    );

    private final SessionManager sessionManager;

    GeneralInfoType(@NotNull SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void execute(@NotNull Player user, @NotNull Player target) {
        Set<String> channels = PacketQueue.get(target).channels();
        Set<String> namespaces = channels.stream().map(s -> s.split(":")[0]).collect(Collectors.toSet());
        String brand = Objects.requireNonNullElse(target.getTag(CLIENT_BRAND), "Unknown");

        var session = sessionManager.getSession(target.getUuid().toString());
        int protocolVersion = OpUtils.mapOr(session, PlayerSession::protocolVersion, -1);

        Component info = Component.empty()
                .append(Component.text("Player info for %s".formatted(target.getUsername()))).appendNewline()
                .append(Component.text("Version: ")).append(Component.text(ProtocolVersions.getProtocolName(protocolVersion) + " (" + protocolVersion + ")")).appendNewline()
                .append(Component.text("Settings: ")).append(Component.text(target.getSettings().toString()))
                .appendNewline()
                .append(Component.text("Brand: ")).append(Component.text(brand))
                .appendNewline()
                .append(Component.text("Mods:"))
                .appendNewline();

        for (String mod : KNOWN_MODS) {
            boolean present = channels.contains(mod) || namespaces.contains(mod);
            info = info.append(Component.text(" - %s: ".formatted(mod))
                    .append(Component.text(present ? "Present" : "Not Present").color(present ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(Component.newline())
            );
        }

        user.sendMessage(info.appendNewline());
    }
}
