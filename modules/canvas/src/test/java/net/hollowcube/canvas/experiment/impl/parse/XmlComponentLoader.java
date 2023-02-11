package net.hollowcube.canvas.experiment.impl.parse;

import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.experiment.impl.AutoLayoutBox;
import net.hollowcube.canvas.experiment.impl.ButtonElement;
import net.hollowcube.canvas.experiment.impl.LabelElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.shaded.org.apache.commons.io.input.ReaderInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.Charset;

public class XmlComponentLoader {

    public static @NotNull Section load(@NotNull String xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new ReaderInputStream(new StringReader(xml), Charset.defaultCharset()));
        doc.getDocumentElement().normalize();

        return loadSection(doc.getDocumentElement());
    }

    private static @NotNull Section loadSection(@NotNull Node node) {
        return switch (node.getNodeName()) {
            case "component" -> loadComponent(node);
            case "label" -> loadLabel(node);
            case "button" -> loadButton(node);
            default -> throw new IllegalArgumentException("Unknown node type: " + node.getNodeName());
        };
    }

    private static @NotNull Section loadComponent(@NotNull Node node) {
        var section = new AutoLayoutBox(getId(node), getWidth(node), getHeight(node));
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            var child = node.getChildNodes().item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            section.addChild(loadSection(child));
        }
        return section;
    }

    private static @NotNull Section loadLabel(@NotNull Node node) {
        var translationKeyAttr = node.getAttributes().getNamedItem("translationKey");
        if (translationKeyAttr == null) throw new IllegalArgumentException("Label must have a translationKey attribute");
        var translationKey = translationKeyAttr.getNodeValue();
        return new LabelElement(getId(node), getWidth(node), getHeight(node), translationKey);
    }

    private static @NotNull Section loadButton(@NotNull Node node) {
        var translationKeyAttr = node.getAttributes().getNamedItem("translationKey");
        if (translationKeyAttr == null) throw new IllegalArgumentException("Button must have a translationKey attribute");
        var translationKey = translationKeyAttr.getNodeValue();
        return new ButtonElement(getId(node), getWidth(node), getHeight(node), translationKey);
    }

    private static @Nullable String getId(@NotNull Node node) {
        var attr = node.getAttributes().getNamedItem("id");
        if (attr == null) return null;
        return attr.getNodeValue();
    }

    private static int getWidth(@NotNull Node node) {
        var attr = node.getAttributes().getNamedItem("width");
        if (attr == null) return 1;
        return Integer.parseInt(attr.getNodeValue());
    }

    private static int getHeight(@NotNull Node node) {
        var attr = node.getAttributes().getNamedItem("height");
        if (attr == null) return 1;
        return Integer.parseInt(attr.getNodeValue());
    }

    public static void main(String[] args) throws Exception {
        var component = """
                <component width="9" height="1" align="ltr">
                    <button id="decr" width="4" translationKey="gui.example.decrement" />
                    <label id="count" translationKey="gui.example.count" />
                    <button id="incr" width="4" translationKey="gui.example.increment" />
                </component>
                """;
        System.out.println(load(component));
    }
}
