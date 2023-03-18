package net.hollowcube.canvas.internal.standalone;

import com.google.common.base.Splitter;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
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

    private static final Map<String, BaseElement> xmlCache = new ConcurrentHashMap<>();

    public static @NotNull BaseElement load(@NotNull String viewPath, boolean cache) {
        if (cache && xmlCache.containsKey(viewPath)) {
            logger.log(System.Logger.Level.DEBUG, "Cache hit for '{0}'", viewPath);
            return xmlCache.get(viewPath).clone();
        }

        var reader = new XmlElementReader(viewPath);
        var root = reader.readRoot();

        xmlCache.put(viewPath, root);
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
            case "switch" -> loadSwitch(node);
            default -> loadImportedElement(node);
        };
    }

    private @NotNull BaseElement loadBox(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("box"), "Node must be `box`");
        var elem = new BoxElement(getId(node), getWidth(node), getHeight(node),
                getEnum(node, "align", BoxElement.Align.LTR));

        return applyTraits(node, loadChildren(node, elem));
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

    private @NotNull BaseElement loadSwitch(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("switch"), "Node must be `switch`");
        var elem = new SwitchElement(getId(node), getWidth(node), getHeight(node));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseParentWithChildrenElement loadChildren(@NotNull Node node, @NotNull BaseParentWithChildrenElement elem) {
        depth++;
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            var child = node.getChildNodes().item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            elem.addChild(loadElement(child));
        }
        depth--;
        return elem;
    }

    private @NotNull BaseElement applyTraits(@NotNull Node node, @NotNull BaseElement elem) {
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

    private @NotNull BaseElement loadImportedElement(@NotNull Node node) {
        for (var importPath : imports) {
            var path = importPath + "." + node.getNodeName();
            try {
                var clazz = Class.forName(path);
                if (!View.class.isAssignableFrom(clazz)) {
                    throw new IllegalArgumentException("Class must extend View: " + path);
                }

                var constructor = clazz.getConstructor();
                var importedElement = (BaseElement) ((View) constructor.newInstance()).element();
                if (importedElement instanceof RootElement importedRoot) {
                    importedRoot.setId(getId(node));
                } else {
                    //todo this is probably not a great requirement, but maybe doesnt matter.
                    // generally i am not a fan of the concept of a setId method.
                    throw new IllegalArgumentException("Imported element must be a root: " + path);
                }
                return importedElement;
            } catch (ClassNotFoundException ignored) {
                // No such class is fine, try the next import
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("View class must have a no-args constructor: " + path);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new IllegalArgumentException("View class constructor threw an exception: " + path, e);
            }
        }
        throw new IllegalArgumentException("Unknown node type: " + node.getNodeName());
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
