package net.hollowcube.canvas.experiment;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.SectionLike;
import net.hollowcube.canvas.experiment.annotation.Action;
import net.hollowcube.canvas.experiment.annotation.Outlet;
import net.hollowcube.canvas.experiment.impl.ButtonElement;
import net.hollowcube.canvas.experiment.impl.Element;
import net.hollowcube.canvas.experiment.impl.parse.XmlComponentLoader;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public abstract class View implements SectionLike {

    public static @NotNull Collector<View, ?, View> autoLayout(int width, int height) {
        throw new UnsupportedOperationException("not implemented");
    }

    private final Section section;

    protected View() {
        try {
            var viewFile = getClass().getResource(String.format("/%s.xml", getClass().getName().replace(".", "/")));
            Check.notNull(viewFile, "View file not found");

            Element root;
            try (var reader = new BufferedReader(new InputStreamReader(viewFile.openStream()))) {
                var content = reader.lines().collect(Collectors.joining("\n"));
                root = (Element) XmlComponentLoader.load(content);
            }

            for (var field : getClass().getDeclaredFields()) {
                var outlet = field.getAnnotation(Outlet.class);
                if (outlet == null) continue;

                var name = outlet.value();
                var element = root.findById(name);
                Check.notNull(element, "Outlet not found: " + name);

                field.setAccessible(true);
                field.set(this, element);
            }

            for (var method : getClass().getDeclaredMethods()) {
                var action = method.getAnnotation(Action.class);
                if (action == null) continue;

                var name = action.value();
                var element = root.findById(name);
                Check.notNull(element, "Action not found: " + name);
                Check.stateCondition(!(element instanceof ButtonElement), "Element is not a button: " + name);

                method.setAccessible(true);
                ((ButtonElement) element).addHandler((unused1, unused2, unused3) -> {
                    try {
                        method.invoke(this);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    return ClickHandler.DENY;
                });
            }

            this.section = (Section) root;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Section section() {
        return section;
    }

    protected void mount() {}

}
