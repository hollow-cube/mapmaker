package a;

import net.kyori.adventure.text.Component;
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

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class Main {
    public static void main(String[] args) {
        var dst = MiniMessage.miniMessage().deserializeToTree("<red><!unknown> <!unknown2> hello <!></red>", TagResolver.builder()
                .resolver(new TagResolver() {
                    @Override
                    public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException {
                        if (!name.startsWith("!")) return null;

                        return Tag.inserting(Component.text("def"));
                    }

                    @Override
                    public boolean has(@NotNull String name) {
                        return name.startsWith("!") && name.length() > 1;
                    }
                })
                .build());
        var result = treeToComponent((ElementNode) dst);
        System.out.println(PlainTextComponentSerializer.plainText().serialize(result));
    }

    private static @NotNull Component treeToComponent(final @NotNull ElementNode node) {
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

            if (tag instanceof Inserting) {
                comp = ((Inserting) tag).value();
            }
        }

        if (!node.unsafeChildren().isEmpty()) {
            final List<Component> children = new ArrayList<>(comp.children().size() + node.children().size());
            children.addAll(comp.children());
            for (final ElementNode child : node.unsafeChildren()) {
                children.add(treeToComponent(child));
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
