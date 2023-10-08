package net.hollowcube.command;

import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CommandDoc(
        @NotNull String name,
        @Nullable String description,
        @Nullable List<Argument> arguments,
        @Nullable List<String> examples
) {

    public record Argument(
            @NotNull String name,
            boolean optional,
            @Nullable String defaultValue,
            @Nullable String description
    ) {
    }

    /**
     * Default doc renderer, todo write format here
     */
    static @NotNull DocRenderer defaultRenderer() {
        return new DefaultRenderer();
    }

    /**
     * A DocRenderer is responsible for writing a {@link CommandDoc} to a sender.
     */
    @FunctionalInterface
    public interface DocRenderer {

        void render(@NotNull CommandSender sender, @NotNull List<String> path, @NotNull CommandDoc doc);
    }

    private static class DefaultRenderer implements DocRenderer {

        @Override
        public void render(@NotNull CommandSender sender, @NotNull List<String> path, @NotNull CommandDoc doc) {
            var builder = Component.text();
            builder.append(Component.text("------ Help for /" + java.lang.String.join(" ", path) + " ------"));
            if (doc.description != null)
                builder.appendNewline().append(Component.text(doc.description));
            builder.appendNewline().append(Component.text("ᴜѕᴀɢᴇ: /" + java.lang.String.join(" ", path)));
            if (doc.arguments != null && !doc.arguments.isEmpty()) {
                for (var argument : doc.arguments) {
                    builder.append(Component.text(" <" + argument.name + ">"));
                }
                builder.appendNewline().append(Component.text("ᴀʀɢᴜᴍᴇɴᴛѕ:"));
                for (var argument : doc.arguments) {
                    builder.appendNewline().append(Component.text(" " + argument.name));
                    if (argument.defaultValue != null)
                        builder.append(Component.text(" (ᴅᴇꜰᴀᴜʟᴛ " + argument.defaultValue + ")"));
                    if (argument.description != null)
                        builder.append(Component.text(": " + argument.description));
                }
            }
            if (doc.examples != null && !doc.examples.isEmpty()) {
                builder.appendNewline().append(Component.text("ᴇхᴀᴍᴘʟᴇѕ:"));
                for (var example : doc.examples) {
                    builder.appendNewline().append(Component.text(" " + example));
                }
            }

            sender.sendMessage(builder.build());
        }
    }

}
