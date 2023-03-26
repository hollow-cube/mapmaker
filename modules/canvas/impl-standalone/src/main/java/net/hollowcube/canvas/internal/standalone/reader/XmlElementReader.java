package net.hollowcube.canvas.internal.standalone.reader;

import net.hollowcube.canvas.internal.standalone.*;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class XmlElementReader {
    private static final System.Logger logger = System.getLogger(XmlElementReader.class.getName());

    private static final Map<String, ViewContainer> xmlCache = new ConcurrentHashMap<>();

    public static @NotNull ViewContainer load(@NotNull String viewPath, boolean cache) {
        if (cache && xmlCache.containsKey(viewPath)) {
            logger.log(System.Logger.Level.DEBUG, "Cache hit for '{0}'", viewPath);
            return xmlCache.get(viewPath);
        }

        var reader = new XmlElementReader(viewPath);
        var root = reader.readRoot();

        if (cache) {
            // If caching, we re clone the root to normalize to always using cloned versions.
            xmlCache.put(viewPath, root);
            root = root.dup();
        }
        return root;
    }

    private final Document doc;
    private int depth = 0;

    private List<String> imports = new ArrayList<>();

    public XmlElementReader(@NotNull String viewPath) {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = builder.parse(viewPath);
            doc.getDocumentElement().normalize();
            parseImports();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load document", e);
        }
    }

    public @NotNull ViewContainer readRoot() {
        return loadRoot(doc.getDocumentElement());
    }

    private @NotNull ViewContainer loadRoot(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("component"), "Root node must be 'component'");
        var elem = new ViewContainer(getId(node), getWidth(node), getHeight(node),
                getEnum(node, "align", BoxContainer.Align.LTR));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadElement(@NotNull Node node) {
        return switch (node.getNodeName()) {
            case "component" -> throw new IllegalStateException("There may only be one root component");
            case "box" -> loadBox(node);
            case "label" -> loadLabel(node);
            case "button" -> loadButton(node);
            default -> throw new IllegalStateException("Unknown element type: " + node.getNodeName());
        };
    }

    private @NotNull BaseElement loadBox(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("box"), "Node must be `box`");
        var elem = new BoxContainer(getId(node), getWidth(node), getHeight(node),
                getEnum(node, "align", BoxContainer.Align.LTR));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadLabel(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("label"), "Node must be `label`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var elem = new LabelElement(getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadButton(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("button"), "Node must be `button`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var elem = new ButtonElement(getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    // Container loading

    private <T extends ContainerElement> T loadChildren(@NotNull Node node, @NotNull T elem) {
        depth++;
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            var child = node.getChildNodes().item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE)
                continue;

            elem.addChild(loadElement(child));
        }
        depth--;
        return elem;
    }

    // Traits

    private <T extends BaseElement> T applyTraits(@NotNull Node node, @NotNull T elem) {
        return elem;
    }

    // Imports

    private void parseImports() {
        var children = doc.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            var child = children.item(i);
            if (child.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE)
                continue;

            String name = child.getNodeName(), value = child.getNodeValue();
            if (!name.equals("import")) continue;

            value = value.trim();
            logger.log(System.Logger.Level.DEBUG, "Importing '" + value + "'");
            imports.add(value);
        }
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
        var value = getString(node, name, null);
        if (value == null) return def;
        return Enum.valueOf(def.getDeclaringClass(), value.toUpperCase());
    }


}
