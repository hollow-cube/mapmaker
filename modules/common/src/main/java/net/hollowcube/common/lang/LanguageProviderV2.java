package net.hollowcube.common.lang;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.internal.parser.node.ElementNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.TagNode;
import net.kyori.adventure.text.minimessage.internal.parser.node.ValueNode;
import net.kyori.adventure.text.minimessage.tag.Inserting;
import net.kyori.adventure.text.minimessage.tag.Modifying;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static net.kyori.adventure.text.Component.translatable;

@SuppressWarnings("UnstableApiUsage")
public class LanguageProviderV2 {

    private static final Logger logger = LoggerFactory.getLogger("LanguageProvider");

    private static final Pattern ARG_PATTERN = Pattern.compile("<(?<index>[0-9]+)(?::(?<format>[0#.,E;\\-%?¤X']+))?>");
    private static final Map<String, NumberFormat> NUMBER_FORMATTERS = new ConcurrentHashMap<>();

    private static final JsonObject langData;

    static {
        JsonObject lang = new JsonObject();
        try (InputStream is = LanguageProviderV2.class.getResourceAsStream("/en_US.json")) {
            if (is != null) {
                lang = new Gson().fromJson(new InputStreamReader(is), JsonObject.class);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (var entry : lang.entrySet()) {
            var value = entry.getValue();
            Iterable<JsonElement> elements = value.isJsonPrimitive() ? List.of(value) : value.getAsJsonArray();
            for (var element : elements) {
                if (!element.isJsonPrimitive()) continue;
                ARG_PATTERN.matcher(element.getAsString())
                    .results()
                    .forEachOrdered(match -> {
                        var format = match.group("format");
                        if (format == null || NUMBER_FORMATTERS.containsKey(format)) return;
                        NUMBER_FORMATTERS.put(format, new DecimalFormat(format));
                    });
            }
        }

        langData = lang;
    }

    // Stores all the partially parsed components, or null if there is no associated translation key.
    private static final Map<String, @Nullable ElementNode> componentCache = new ConcurrentHashMap<>();
    private static final Map<String, @Nullable List<ElementNode>> multiComponentCache = new ConcurrentHashMap<>();

    private static final Cache<TranslatableComponent, Component> expandedComponentCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .build();

    public static @NotNull String translateToPlain(@NotNull String translationKey) {
        return PLAIN_TEXT.serialize(translate(translatable(translationKey)));
    }

    public static @NotNull String translateToPlain(@NotNull Component component) {
        if (!(component instanceof TranslatableComponent translatable))
            return PLAIN_TEXT.serialize(Objects.requireNonNull(component));
        return PLAIN_TEXT.serialize(translate(translatable));
    }

    @Contract("!null -> !null")
    public static @Nullable Component translate(@Nullable Component component) {
        if (component == null) return null;
        if (!(component instanceof TranslatableComponent translatable)) {
            // Minestom seems not to check for children so we do it here, but this is probably insanely slow for the 99% of cases we dont need it...
            return component.children(component.children().stream()
                .map(LanguageProviderV2::translate)
                .toList());
        }

        // Return from cache if present
        var cached = expandedComponentCache.getIfPresent(translatable);
        if (cached != null) return cached;
//        logger.debug("Cache miss for {}", translatable.key());

        // Fetch the partially parsed minimessage tree, or return the translatable if we dont know about it,
        // for example we need to pass through `chat.type.text` and others.
        var partial = componentCache.computeIfAbsent(translatable.key(), LanguageProviderV2::parseComponent);
        if (partial == null) return translatable;

        // Apply the args to the partial (after translating the args)
        var args = translatable.arguments().stream()
            .map(translationArgument -> translate(translationArgument.asComponent()))
            .toList();
        Component result;
        try {
            result = BASE_EMPTY.append(treeToComponent(partial, args));
        } catch (IllegalStateException e) {
            logger.warn("Forced to fully reparse {} due to {}", translatable.key(), e.getMessage());
            partial = Objects.requireNonNull(parseComponent(translatable.key()));
            result = BASE_EMPTY.append(treeToComponent(partial, args));
        }
        expandedComponentCache.put(translatable, result);
        return result;
    }

    public static @NotNull List<Component> translateMulti(@NotNull String key, @NotNull List<? extends ComponentLike> args) {
        var partials = multiComponentCache.computeIfAbsent(key, LanguageProviderV2::parseMultiComponent);
        if (partials == null) return List.of(); // Must return empty because we use it for lore which is not required.

        // Apply the args to the partials (after translating the args)
        var translatedArgs = args.stream()
            .map(ComponentLike::asComponent)
            .map(LanguageProviderV2::translate)
            .toList();
        return partials.stream()
            .map(partial -> BASE_EMPTY.append(treeToComponent(partial, translatedArgs)))
            .toList();
    }

    public static @NotNull Component translateMultiMerged(@NotNull String key, @NotNull List<? extends ComponentLike> args) {
        var partials = translateMulti(key, args);
        if (partials.isEmpty()) return Component.empty();
        if (partials.size() == 1) return partials.get(0);

        var result = Component.text();
        result.append(partials.get(0));
        for (int i = 1; i < partials.size(); i++)
            result.appendNewline().append(partials.get(i));
        return result.build();
    }

    public static @NotNull Component getVanillaTranslation(@NotNull Material material) {
        if (material.isBlock()) return getVanillaTranslation(material.registry().block());
        return translatable("item." + material.key().namespace() + "." + material.key().value());
    }

    public static @NotNull Component getVanillaTranslation(@NotNull Block block) {
        return translatable("block." + block.key().namespace() + "." + block.key().value());
    }

    public static boolean hasTranslationKey(@NotNull String key) {
        return langData.has(key);
    }

    // Use of a lot of internal Minimessage APIs below. May break in the future and need to write this ourselves.

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
        .build();

    public static final Component BASE_EMPTY = Component.text("", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);

    private record PlaceholderTag(int index, @Nullable NumberFormat formatter) implements Tag {
        private static final TagResolver RESOLVER = new TagResolver() {
            @Override
            public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
                try {
                    var index = Integer.parseInt(name);
                    if (index < 0) return null;
                    return new PlaceholderTag(index, arguments.hasNext() ? NUMBER_FORMATTERS.get(arguments.pop().value()) : null);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }

            @Override
            public boolean has(@NotNull String name) {
                try {
                    return Integer.parseInt(name) >= 0;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        };
    }

    static @Nullable ElementNode parseComponent(@NotNull String id) {
        var raw = langData.get(id);
        if (raw == null) return null;

        return deserializeToTree(raw.isJsonPrimitive() ? raw.getAsString() : raw.getAsJsonArray().get(0).getAsString());
    }

    private static @Nullable List<ElementNode> parseMultiComponent(String key) {
        var raw = langData.get(key);
        if (raw == null) return null;

        JsonArray value;
        if (raw.isJsonArray()) {
            value = raw.getAsJsonArray();
        } else {
            value = new JsonArray();
            value.add(raw);
        }

        var result = new ArrayList<ElementNode>();
        for (var entry : value) {
            result.add(deserializeToTree(entry.getAsString()));
        }
        return result;
    }

    static @NotNull ElementNode deserializeToTree(@NotNull String value) {
        return (ElementNode) MINI_MESSAGE.deserializeToTree(value, PlaceholderTag.RESOLVER, MyHoverTag.RESOLVER, MyClickTag.RESOLVER);
    }

    static @NotNull String replaceInString(@NotNull String value, @NotNull List<Component> args) {
        return ARG_PATTERN.matcher(value)
            .replaceAll(match -> {
                var index = Integer.parseInt(match.group("index"));
                if (index < 0 || index >= args.size()) {
                    return "\\$\\$" + index;
                } else {
                    var component = args.get(index);
                    var format = match.group("format");
                    var formatter = format == null ? null : NUMBER_FORMATTERS.get(format);
                    if (formatter != null && component instanceof TranslationArgument argument && argument.value() instanceof Number number) {
                        return formatter.format(number);
                    }
                    return PLAIN_TEXT.serialize(component);
                }
            });
    }

    // The following are taken directly from MiniMessageParser inside minimessage. It is an internal API.

    static @NotNull Component treeToComponent(final @NotNull ElementNode node, @NotNull List<Component> args) {
        Component comp = Component.empty();
        Tag tag = null;
        if (node instanceof ValueNode) {
            comp = Component.text(((ValueNode) node).value());
        } else if (node instanceof TagNode) {
            final TagNode tagNode = (TagNode) node;

            tag = tagNode.tag();

            // special case for gradient and stuff
            if (tag instanceof Modifying modTransformation) {
                // first walk the tree
                visitModifying(modTransformation, tagNode, 0);
                modTransformation.postVisit();
            }

            if (tag instanceof Inserting insertingTag) {
                comp = insertingTag.value();
            }
            if (tag instanceof InsertingWithArgs insertingTag) {
                comp = insertingTag.value(args);
            }

            if (tag instanceof PlaceholderTag placeholderTag) {
                if (placeholderTag.index >= args.size()) {
                    comp = Component.text("$$" + placeholderTag.index);
                } else {
                    comp = args.get(placeholderTag.index);
                    if (placeholderTag.formatter != null) {
                        // We need to parse the number from the component and format it being changed to Component.text()
                        Number number = switch (comp) {
                            case TranslationArgument argument -> {
                                var value = argument.value();
                                yield value instanceof Number ? (Number) value : null;
                            }
                            case TextComponent text -> {
                                try {
                                    yield Double.parseDouble(text.content());
                                } catch (NumberFormatException e) {
                                    yield null;
                                }
                            }
                            default -> null;
                        };
                        if (number != null) comp = Component.text(placeholderTag.formatter.format(number));
                    }
                }
            }
        }

        if (!node.unsafeChildren().isEmpty()) {
            final List<Component> children = new ArrayList<>(comp.children().size() + node.children().size());
            children.addAll(comp.children());
            for (final ElementNode child : node.unsafeChildren()) {
                children.add(treeToComponent(child, args));
            }
            comp = comp.children(children);
        }

        // special case for gradient and stuff
        if (tag instanceof Modifying) {
            comp = handleModifying((Modifying) tag, comp, 0);
        }

        return comp;
    }

    private static void visitModifying(final Modifying modTransformation, final ElementNode node, final int depth) {
        modTransformation.visit(node, depth);
        for (final ElementNode child : node.unsafeChildren()) {
            visitModifying(modTransformation, child, depth + 1);
        }
    }

    private static Component handleModifying(final Modifying modTransformation, final Component current, final int depth) {
        Component newComp = modTransformation.apply(current, depth);
        for (final Component child : current.children()) {
            newComp = newComp.append(handleModifying(modTransformation, child, depth + 1));
        }
        return newComp;
    }

}
