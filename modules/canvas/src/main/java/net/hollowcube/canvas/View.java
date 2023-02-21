package net.hollowcube.canvas;

import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.standalone.BaseElement;
import net.hollowcube.canvas.internal.standalone.RootElement;
import net.hollowcube.canvas.internal.standalone.XmlElementReader;
import net.hollowcube.canvas.section.Section;
import net.hollowcube.canvas.section.SectionLike;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public abstract class View implements SectionLike {

    private final BaseElement root;

    protected View() {
        var viewFile = getClass().getResource(String.format("/%s.xml", getClass().getName().replace(".", "/")));
        Check.notNull(viewFile, "View file not found: " + getClass().getName() + ".xml");

        root = XmlElementReader.load(viewFile.toString(), true);
        wireOutlets();
        wireActions();

        if (root instanceof RootElement elem) {
            elem.addMountHandler(this::mount);
        } else {
            throw new RuntimeException("Root is not a RootElement");
        }
    }

    @Override
    public @NotNull Section section() {
        return root.section();
    }

    public BaseElement getRoot() {
        return root;
    }

    protected void mount() {

    }

    private void wireOutlets() {
        try {
            for (var field : getClass().getDeclaredFields()) {
                var outlet = field.getAnnotation(Outlet.class);
                if (outlet == null) continue;

                var name = outlet.value();
                var element = root.findById(name);
                Check.notNull(element, "Outlet not found: " + name);

                if (!field.getType().isAssignableFrom(element.getClass())) {
                    throw new RuntimeException("Outlet type mismatch: " + name);
                }

                field.setAccessible(true);
                field.set(this, element);
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void wireActions() {
        for (var method : getClass().getDeclaredMethods()) {
            var action = method.getAnnotation(Action.class);
            if (action == null) continue;

            var name = action.value();
            var element = root.findById(name);
            Check.notNull(element, "Action not found: " + name);

            if (element instanceof BaseElement baseElement) {
                baseElement.wireAction(this, method);
            } else {
                throw new RuntimeException("todo");
            }
        }
    }

}
