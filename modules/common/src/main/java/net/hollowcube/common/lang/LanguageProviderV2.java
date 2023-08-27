package net.hollowcube.common.lang;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public class LanguageProviderV2 {

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
        langData = lang;
    }

    // Stores all the partially parsed components, or null if there is no associated translation key.
    private static final Map<String, @Nullable ElementNode> componentCache = new ConcurrentHashMap<>();
    private static final Map<String, @Nullable List<ElementNode>> multiComponentCache = new ConcurrentHashMap<>();

    public static @NotNull Component translate(@NotNull Component component) {
        if (!(component instanceof TranslatableComponent translatable)) {
            return component;
        }

        // Fetch the partially parsed minimessage tree, or return the translatable if we dont know about it,
        // for example we need to pass through `chat.type.text` and others.
        var partial = componentCache.computeIfAbsent(translatable.key(), LanguageProviderV2::parseComponent);
        if (partial == null) return translatable;

        // Apply the args to the partial (after translating the args)
        var args = translatable.args().stream()
                .map(LanguageProviderV2::translate)
                .toList();
        return BASE_EMPTY.append(treeToComponent(partial, args));
    }

    public static @NotNull List<Component> translateMulti(@NotNull String key, @NotNull List<Component> args) {
        var partials = multiComponentCache.computeIfAbsent(key, LanguageProviderV2::parseMultiComponent);
        if (partials == null) return List.of(); // Must return empty because we use it for lore which is not required.

        // Apply the args to the partials (after translating the args)
        var translatedArgs = args.stream()
                .map(LanguageProviderV2::translate)
                .toList();
        return partials.stream()
                .map(partial -> BASE_EMPTY.append(treeToComponent(partial, translatedArgs)))
                .toList();
    }

    // Use of a lot of internal Minimessage APIs below. May break in the future and need to write this ourselves.

    private static final Pattern ARG_PATTERN = Pattern.compile("<[0-9]+>");
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder()
            .build();

    private static final Component BASE_EMPTY = Component.text("", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);

    private record PlaceholderTag(int index) implements Tag {
        private static final TagResolver RESOLVER = new TagResolver() {
            @Override
            public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
                try {
                    var index = Integer.parseInt(name);
                    if (index < 0) return null;
                    return new PlaceholderTag(index);
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
                    var rawGroup = match.group();
                    var index = Integer.parseInt(rawGroup.substring(1, rawGroup.length() - 1));
                    if (index < 0 || index >= args.size()) {
                        return "$$" + index;
                    } else {
                        return PLAIN_TEXT.serialize(args.get(index));
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
            if (tag instanceof Modifying) {
                final Modifying modTransformation = (Modifying) tag;

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
                if (placeholderTag.index >= args.size())
                    comp = Component.text("$$" + placeholderTag.index);
                else comp = args.get(placeholderTag.index);
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
