package net.hollowcube.canvas.internal.standalone.reader;

import net.hollowcube.canvas.internal.standalone.ViewContainer;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            root = root.clone();
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


}
