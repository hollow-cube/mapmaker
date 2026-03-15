package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.mapmaker.util.thesneaky.TheSneaky;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;

class ReportsInfoType extends PlayerInfoType.ForPlayer {

    @Override
    public void execute(Player user, Player target) {
        TheSneaky.test(target).thenAccept(report -> {
            if (report == null) {
                user.sendMessage("No info reports found for %s".formatted(target.getUsername()));
            } else {
                Component message = Component.text("Info Reports for %s".formatted(target.getUsername()))
                    .appendNewline()
                    .append(
                        Component.text("Mod cleared translations: ")
                            .append(Component.text(report.aModClearedTranslations() ? "Yes" : "No").color(report.aModClearedTranslations() ? NamedTextColor.GREEN : NamedTextColor.RED))
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

                    message = message.append(
                        Component.text(" - %s: ".formatted(entry.getKey()))
                            .append(status)
                            .append(Component.newline())
                    );
                }

                user.sendMessage(message);
            }
        });
    }
}
