package net.hollowcube.map.item;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.builder.suggestion.Suggestion;
import net.minestom.server.command.builder.suggestion.SuggestionEntry;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages custom items for mapmaker maps.
 * todo make this not a singleton
 */
public class ItemManager {
    private static final Map<NamespaceID, ItemStack> items = new HashMap<>();

    static {
        for (var material : Material.values()) {
            items.put(material.namespace(), ItemStack.of(material));
        }
    }

    public static final Argument<@Nullable ItemStack> ARGUMENT = ArgumentType.ResourceLocation("item")
            .setSuggestionCallback(ItemManager::commandCompleter)
            .map(ItemManager::getItem);

    public static void register(@NotNull NamespaceID namespace, @NotNull ItemStack item) {
        items.put(namespace, item);
    }

    private static void commandCompleter(@NotNull CommandSender sender, @NotNull CommandContext context, @NotNull Suggestion suggestion) {
        var input = suggestion.getInput().substring(suggestion.getStart() - 1).trim();

        if (input.isBlank()) {
            items.keySet().stream()
                    .limit(20)
                    .forEach(name -> suggestion.addEntry(new SuggestionEntry(name.toString())));
            return;
        }

        items.keySet().stream()
                .filter(name -> name.namespace().startsWith(input) || name.path().startsWith(input))
                .limit(20)
                .forEach(name -> suggestion.addEntry(new SuggestionEntry(name.toString())));
    }

    private static @Nullable ItemStack getItem(@NotNull String name) {
        var namespace = NamespaceID.from(name);
        if (items.containsKey(namespace)) {
            return items.get(namespace);
        }

        var material = Material.fromNamespaceId(namespace);
        if (material != null) {
            return ItemStack.of(material);
        }

        return null;
    }
}
