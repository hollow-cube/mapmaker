package net.hollowcube.canvas.internal.standalone.reader;

import com.google.common.base.Splitter;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.standalone.*;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.hollowcube.canvas.internal.standalone.util.Debugger;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class XmlElementReader {
    private static final System.Logger logger = System.getLogger(XmlElementReader.class.getName());

    private static final Map<String, ViewContainer> xmlCache = new ConcurrentHashMap<>();

    public static @NotNull ViewContainer load(@NotNull ElementContext context, @NotNull String viewPath, boolean cache) {
        if (cache && xmlCache.containsKey(viewPath)) {
            logger.log(System.Logger.Level.DEBUG, "Cache hit for '{0}'", viewPath);
            return xmlCache.get(viewPath).clone(context);
        }

        var reader = new XmlElementReader(context, viewPath);
        var root = reader.readRoot();

        if (cache && !Debugger.isEnabled()) { // Never cache when debugging
            // If caching, we re clone the root to normalize to always using cloned versions.
            xmlCache.put(viewPath, root);
            root = root.clone(context);
        }
        return root;
    }

    private final ElementContext context;
    private final Document doc;
    private int depth = 0;

    private List<String> imports = new ArrayList<>();

    public XmlElementReader(@NotNull ElementContext context, @NotNull String viewPath) {
        this.context = context;
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
        var elem = new ViewContainer(context, getId(node), getWidth(node), getHeight(node),
                getEnum(node, "align", BoxContainer.Align.LTR));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadElement(@NotNull Node node) {
        return switch (node.getNodeName()) {
            case "component" -> throw new IllegalStateException("There may only be one root component");
            case "box" -> loadBox(node);
            case "label" -> loadLabel(node);
            case "button" -> loadButton(node);
            case "spacer" -> loadSpacer(node);
            case "switch" -> loadSwitch(node);
            case "pagination" -> loadPagination(node);
            default -> loadImportedElement(node);
        };
    }

    private @NotNull BaseElement loadBox(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("box"), "Node must be `box`");
        var elem = new BoxContainer(context, getId(node), getWidth(node), getHeight(node),
                getEnum(node, "align", BoxContainer.Align.LTR));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadLabel(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("label"), "Node must be `label`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var elem = new LabelElement(context, getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadButton(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("button"), "Node must be `button`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var elem = new ButtonElement(context, getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadSpacer(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("spacer"), "Node must be `spacer`");
        return new SpacerElement(context, getWidth(node), getHeight(node));
    }

    private @NotNull BaseElement loadSwitch(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("switch"), "Node must be `switch`");
        var elem = new SwitchElement(context, getId(node), getWidth(node), getHeight(node));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadPagination(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("pagination"), "Node must be `pagination`");

        var itemClass = findImportedClass(node);
        var elem = new PaginationElement(context, getId(node), getWidth(node), getHeight(node), itemClass);
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadImportedElement(@NotNull Node node) {
        var clazz = findImportedClass(node);
        try {
            var constructor = clazz.getConstructor(Context.class);
            var importedElement = (ViewContainer) constructor.newInstance(context).element();
            importedElement.setId(getId(node));
            return importedElement;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("View class must have a no-args constructor: " + clazz);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("View class constructor threw an exception: " + clazz, e);
        }
    }

    private @NotNull Class<? extends View> findImportedClass(@NotNull Node node) {
        for (var importPath : imports) {
            var path = importPath + "." + node.getNodeName();
            try {
                var clazz = Class.forName(path);
                if (!View.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("Class must extend View: " + path);
                }

                //noinspection unchecked
                return (Class<? extends View>) clazz;
            } catch (ClassNotFoundException ignored) {
                // No such class is fine, try the next import
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + node.getNodeName());
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
        if (elem instanceof DepthAware trait) {
            trait.setZIndex(depth);
        }

        // Sprites
        var spriteName = getString(node, "sprite", null);
        if (spriteName != null) {
            var sprite = Sprite.SPRITE_MAP.get(spriteName);
            if (sprite != null) {
                if (elem instanceof SpriteHolder trait) {
                    trait.setSprite(sprite);
                } else {
                    throw new IllegalArgumentException("Element does not support sprites: " + elem.getClass().getSimpleName());
                }
            } else {
                // Attempt to parse the sprite as an item/cmd in the form `minecraft:stick@1000`
                var split = Splitter.on('@').splitToList(spriteName);
                var material = Material.fromNamespaceId(split.get(0));
                if (material == null) {
                    throw new IllegalArgumentException("Unknown sprite: " + spriteName);
                }
                var builder = ItemStack.builder(material);
                if (split.size() > 1) {
                    builder.meta(meta -> meta.customModelData(Integer.parseInt(split.get(1))));
                }
                if (elem instanceof ItemSpriteHolder trait) {
                    trait.setItemSprite(builder.build());
                } else {
                    throw new IllegalArgumentException("Element does not support item sprites: " + elem.getClass().getSimpleName());
                }
            }
        }

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
