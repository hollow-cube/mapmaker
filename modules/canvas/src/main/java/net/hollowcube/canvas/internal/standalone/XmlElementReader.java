package net.hollowcube.canvas.internal.standalone;

import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class XmlElementReader {
    private static final Map<String, BaseElement> xmlCache = new ConcurrentHashMap<>();

    public static @NotNull BaseElement load(@NotNull String viewPath, boolean cache) {
        if (cache && xmlCache.containsKey(viewPath)) {
            return xmlCache.get(viewPath).clone();
        }

        var reader = new XmlElementReader(viewPath);
        var root = reader.readRoot();

        xmlCache.put(viewPath, root);
        return root;
    }

    private final Document doc;
    private int depth = 0;

    public XmlElementReader(@NotNull String viewPath) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(viewPath);
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load document", e);
        }
    }

    public @NotNull BaseElement readRoot() {
        return loadRoot(doc.getDocumentElement());
    }

    private @NotNull BaseElement loadRoot(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("component"), "Node must be `component`");
        var elem = new RootElement(getId(node), getWidth(node), getHeight(node),
                getEnum(node, "align", RootElement.Align.LTR));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadElement(@NotNull Node node) {
        return switch (node.getNodeName()) {
            case "component" -> throw new IllegalStateException("Only the root may be `component`");
            case "box" -> loadBox(node);
            case "spacer" -> loadSpacer(node);
            case "label" -> loadLabel(node);
            case "button" -> loadButton(node);
            case "pagination" -> loadPagination(node);
            default -> throw new IllegalArgumentException("Unknown node type: " + node.getNodeName());
        };
    }

    private @NotNull BaseElement loadBox(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("box"), "Node must be `box`");
        var elem = new BoxElement(getId(node), getWidth(node), getHeight(node),
                getEnum(node, "align", BoxElement.Align.LTR));

        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadChildren(@NotNull Node node, @NotNull BoxElement elem) {
        depth++;
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            var child = node.getChildNodes().item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            elem.addChild(loadElement(child));
        }
        depth--;
        return elem;
    }

    private @NotNull BaseElement loadSpacer(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("spacer"), "Node must be `spacer`");
        var elem = new SpacerElement(getWidth(node), getHeight(node));
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadLabel(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("label"), "Node must be `label`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var elem = new ItemLabelElement(getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadButton(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("button"), "Node must be `button`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Button must have a translation key");
        var elem = new ItemButtonElement(getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadPagination(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("pagination"), "Node must be `pagination`");
        var elem = new PaginationElement(getId(node), getWidth(node), getHeight(node));
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement applyTraits(@NotNull Node node, @NotNull BaseElement elem) {
        if (elem instanceof DepthAware trait) {
            trait.setZIndex(depth);
        }
        if (elem instanceof SpriteHolder trair) {
            var sprite = getString(node, "sprite", null);
            if (sprite != null) {
                //todo error if sprite does not exist
                trair.setSprite(Sprite.SPRITE_MAP.get(sprite));
            }
        }

        return elem;
    }

    // Helpers

    private @Nullable String getId(@NotNull Node node) {
        return getString(node, "id", null);
    }

    private int getWidth(@NotNull Node node) {
        return getInt(node, "width", 1);
    }

    private int getHeight(@NotNull Node node) {
        return getInt(node, "height", 1);
    }

    private int getInt(@NotNull Node node, @NotNull String name, int def) {
        var attr = node.getAttributes().getNamedItem(name);
        if (attr == null) return def;
        return Integer.parseInt(attr.getNodeValue());
    }

    private @UnknownNullability String getString(@NotNull Node node, @NotNull String name, @UnknownNullability String def) {
        var attr = node.getAttributes().getNamedItem(name);
        if (attr == null) return def;
        return attr.getNodeValue();
    }

    private <E extends Enum<E>> @NotNull E getEnum(@NotNull Node node, @NotNull String name, @NotNull E def) {
        var value = getString(node, name, (String) null);
        if (value == null) return def;
        return Enum.valueOf(def.getDeclaringClass(), value.toUpperCase());
    }


}
