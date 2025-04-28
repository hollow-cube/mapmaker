package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.common.util.BlockUtil;
import net.kyori.adventure.key.Key;
import net.minestom.server.command.CommandSender;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.stream.Collectors;

public class ArgumentBlock extends Argument<Block> {
    private static final Collection<@NonNull Key> BLOCK_IDS = Block.values().stream()
            .map(Block::key)
            .collect(Collectors.toUnmodifiableSet());

    protected ArgumentBlock(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Block> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var result = BlockUtil.fromString(reader.readWord(WordType.GREEDY).toLowerCase(Locale.ROOT));

        if (result.isLeft()) {
            return success(result.left());
        }

        final BlockUtil.BlockParseResult right = result.right();
        return switch (right) {
            case INVALID_PROPERTIES -> partial();
            case BLOCK_NOT_FOUND -> partial("Unknown block");
            case INVALID_PROPERTY_VALUE -> syntaxError("Invalid block properties");
            case NO_BLOCK_TYPE -> syntaxError("Expected block type");
        };
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        raw = raw.toLowerCase(Locale.ROOT);

        var index = raw.indexOf("[");
        if (index != -1) {
            var blockId = raw.substring(0, index);
            var properties = raw.substring(index + 1);
            var block = Block.fromKey(blockId);

            if (block == null) {
                return;
            }

            var lastPropertyStart = properties.lastIndexOf(",");
            var usedProperties = new HashSet<String>();
            if (lastPropertyStart != -1) {
                var split = properties.substring(0, lastPropertyStart).split(",");
                for (String property : split) {
                    usedProperties.add(property.split("=")[0]);
                }
            }

            var blockProperties = BlockUtil.getBlockProperties(block).entrySet()
                    .stream().filter(entry -> !usedProperties.contains(entry.getKey()));
            suggestion.setStart(suggestion.getStart() + index + 1 + Math.max(0, lastPropertyStart + 1));

            var lastProperty = properties.substring(lastPropertyStart + 1);

            blockProperties.forEach(entry -> {
                if (lastProperty.contains("=") && lastProperty.split("=")[0].equals(entry.getKey())) {
                    suggestion.setStart(suggestion.getStart() + 1 + entry.getKey().length());
                    for (String s : entry.getValue()) {
                        final String[] split = lastProperty.split("=");
                        if (split.length == 2 && s.equals(split[1])) {
                            suggestion.clear();
                            suggestion.setStart(suggestion.getStart() + s.length());
                            suggestion.add("]");
                            suggestion.add(",");
                            return;
                        }
                        if (split.length == 1 || s.startsWith(split[1])) {
                            suggestion.add(s);
                        }
                    }
                    return;
                }
                if (entry.getKey().startsWith(lastProperty)) {
                    suggestion.add(entry.getKey());
                }
                if (entry.getKey().equals(lastProperty)) {
                    suggestion.getEntries().clear();
                    suggestion.setStart(suggestion.getStart() +entry.getKey().length());
                    suggestion.add("=");
                }
            });

            return;
        }

        for (var blockId : BLOCK_IDS) {
            if (blockId.asString().startsWith(raw) || blockId.value().startsWith(raw))
                suggestion.add(blockId.asString());
        }

        if (suggestion.getEntries().size() == 1 && Block.fromKey(raw) != null) {
            suggestion.getEntries().clear();
            suggestion.setStart(suggestion.getStart() + raw.length());
            suggestion.add("[");
        }
    }
}
