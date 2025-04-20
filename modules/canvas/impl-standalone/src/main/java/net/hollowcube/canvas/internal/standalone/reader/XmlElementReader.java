package net.hollowcube.canvas.internal.standalone.reader;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.standalone.*;
import net.hollowcube.canvas.internal.standalone.context.ElementContext;
import net.hollowcube.canvas.internal.standalone.sprite.Sprite;
import net.hollowcube.canvas.internal.standalone.trait.DepthAware;
import net.hollowcube.canvas.internal.standalone.trait.ItemSpriteHolder;
import net.hollowcube.canvas.internal.standalone.trait.Loadable;
import net.hollowcube.canvas.internal.standalone.trait.SpriteHolder;
import net.hollowcube.canvas.internal.standalone.util.Debugger;
import net.minestom.server.component.DataComponents;
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

    private final List<String> imports = new ArrayList<>();

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
                Objects.requireNonNull(getEnum(BoxContainer.Align.class, node, "align", null), "Component must have an alignment"),
                getBool(node, "wipe_player_inv", false));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadElement(@NotNull Node node) {
        return switch (node.getNodeName()) {
            case "component" -> throw new IllegalStateException("There may only be one root component");
            case "box" -> loadBox(node);
            case "label" -> loadLabel(node);
            case "sprite" -> loadSprite(node);
            case "item" -> loadItem(node);
            case "button" -> loadButton(node);
            case "spacer" -> loadSpacer(node);
            case "switch" -> loadSwitch(node);
            case "pagination" -> loadPagination(node);
            case "text" -> loadText(node);
            default -> loadImportedElement(node);
        };
    }

    private @NotNull BaseElement loadBox(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("box"), "Node must be `box`");
        var elem = new BoxContainer(context, getId(node), getWidth(node), getHeight(node),
                Objects.requireNonNull(getEnum(BoxContainer.Align.class, node, "align", null), "Box must have an alignment"));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadLabel(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("label"), "Node must be `label`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var elem = new LabelElement(context, getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadSprite(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("sprite"), "Node must be `sprite`");
        return applyTraits(node, new SpriteElement(context, getId(node)));
    }

    private @NotNull BaseElement loadItem(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("item"), "Node must be `item`");
        return applyTraits(node, new ItemElement(context, getId(node), getWidth(node), getHeight(node)));
    }

    private @NotNull BaseElement loadButton(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("button"), "Node must be `button`");
        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var elem = new ButtonElement(context, getId(node), getWidth(node), getHeight(node), translationKey);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadSpacer(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("spacer"), "Node must be `spacer`");
        var elem = new SpacerElement(context, getWidth(node), getHeight(node));
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadSwitch(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("switch"), "Node must be `switch`");
        var elem = new SwitchElement(context, getId(node), getWidth(node), getHeight(node));
        return applyTraits(node, loadChildren(node, elem));
    }

    private @NotNull BaseElement loadPagination(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("pagination"), "Node must be `pagination`");

        var itemClassName = getString(node, "child", null);
        Check.argCondition(itemClassName == null, "Pagination must have a child class");
        var itemClass = findImportedClass(itemClassName); // NOSONAR - sonarqube doesnt understand contracts
        var isCached = getBool(node, "cached", true);
        var elem = new PaginationElement<>(context, getId(node), getWidth(node), getHeight(node), itemClass, isCached);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadText(@NotNull Node node) {
        Check.argCondition(!node.getNodeName().equals("text"), "Node must be `text`");

        var translationKey = Objects.requireNonNull(getString(node, "translationKey", null), "Label must have a translation key");
        var fontName = getString(node, "font", "default");
        var shift = getInt(node, "shift", 0);
        var centered = getBool(node, "centered");
        var initialValue = getString(node, "value", "");
        var elem = new TextElement(context, getId(node), getWidth(node), getHeight(node),
                translationKey, fontName, shift, centered, initialValue);
        return applyTraits(node, elem);
    }

    private @NotNull BaseElement loadImportedElement(@NotNull Node node) {
        var clazz = findImportedClass(node.getNodeName());
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

    private @NotNull Class<? extends View> findImportedClass(@NotNull String name) {
        for (var importPath : imports) {
            var path = importPath + "." + name;
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
        throw new IllegalArgumentException("Unknown node type: " + name);
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

        // Loading
        var loadingType = getString(node, "loader", null);
        if (loadingType != null) {
            if (elem instanceof Loadable trait) {
                trait.setLoadingType(loadingType);
            } else {
                throw new IllegalArgumentException("Element does not support loading: " + elem.getClass().getSimpleName());
            }
        }

        // Depth
        if (elem instanceof DepthAware trait) {
            trait.setZIndex(depth);
        }

        // Sprites
        var backgroundSpriteName = getString(node, "backgroundSprite", null);
        if (backgroundSpriteName != null) {
            var sprite = Sprite.SPRITE_MAP.get(backgroundSpriteName);
            if (sprite != null) {
                if (sprite.fontChar() != 0) {
                    if (elem instanceof SpriteHolder trait) {
                        trait.setSprite(sprite);
                    } else {
                        throw new IllegalArgumentException("Element does not support sprites: " + elem.getClass().getSimpleName());
                    }
                } else {
                    throw new IllegalArgumentException("Background sprite must be a font sprite: " + backgroundSpriteName);
                }
            } else {
                throw new IllegalArgumentException("Unknown sprite: " + backgroundSpriteName);
            }
        }

        var spriteName = getString(node, "sprite", null);
        if (spriteName != null) {
            Integer spritePos = null;
            var rawSpritePos = node.getAttributes().getNamedItem("spritePos");
            if (rawSpritePos != null) {
                spritePos = Integer.parseInt(rawSpritePos.getNodeValue());
            }

            var sprite = Sprite.SPRITE_MAP.get(spriteName);
            if (sprite != null) {
                if (sprite.fontChar() != 0) {
                    if (elem instanceof SpriteHolder trait) {
                        trait.setSprite(sprite);
                    } else {
                        throw new IllegalArgumentException("Element does not support sprites: " + elem.getClass().getSimpleName());
                    }
                } else {
                    if (elem instanceof ItemSpriteHolder trait) {
                        trait.setItemSprite(ItemStack.builder(Material.DIAMOND)
                                .set(DataComponents.ITEM_MODEL, Objects.requireNonNull(sprite.model(), "Sprite must have a model"))
                                .build(), spritePos);
                    } else {
                        throw new IllegalArgumentException("Element does not support item sprites: " + elem.getClass().getSimpleName());
                    }
                }

            } else {
                if (spriteName.contains("@")) {
                    throw new IllegalArgumentException("Found legacy @ for custom model data, this is no longer supported.");
                }
                var material = Material.fromKey(spriteName);
                if (material == null) {
                    logger.log(System.Logger.Level.WARNING, "Missing sprite: " + spriteName);
                    throw new IllegalArgumentException("Unknown sprite: " + spriteName);
                }
                if (elem instanceof ItemSpriteHolder trait) {
                    trait.setItemSprite(ItemStack.of(material), spritePos);
                } else {
                    throw new IllegalArgumentException("Element does not support item sprites: " + elem.getClass().getSimpleName());
                }
            }
        }
        var loadingSpriteName = getString(node, "loadingSprite", null);
        if (loadingSpriteName != null) {
            var sprite = Sprite.SPRITE_MAP.get(loadingSpriteName);
            if (sprite != null) {
                elem.setLoadingSprite(sprite);
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

    private boolean getBool(@NotNull Node node, @NotNull String name) {
        var attr = node.getAttributes().getNamedItem(name);
        return attr != null && Boolean.parseBoolean(attr.getNodeValue());
    }

    private boolean getBool(@NotNull Node node, @NotNull String name, boolean defaultValue) {
        var attr = node.getAttributes().getNamedItem(name);
        if (attr == null) return defaultValue;
        return Boolean.parseBoolean(attr.getNodeValue());
    }

    private @UnknownNullability String getString(@NotNull Node node, @NotNull String name, @UnknownNullability String def) {
        var attr = node.getAttributes().getNamedItem(name);
        if (attr == null) return def;
        return attr.getNodeValue();
    }

    private <E extends Enum<E>> @UnknownNullability E getEnum(@NotNull Class<E> clazz, @NotNull Node node, @NotNull String name, @UnknownNullability E def) {
        var value = getString(node, name, null);
        if (value == null) return def;
        return Enum.valueOf(clazz, value.toUpperCase());
    }

}
