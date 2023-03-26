package net.hollowcube.canvas.internal.standalone.provider;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import net.hollowcube.canvas.internal.standalone.BaseElement;
import net.hollowcube.canvas.internal.standalone.ViewContainer;
import net.hollowcube.canvas.internal.standalone.context.RenderableContext;
import net.hollowcube.canvas.internal.standalone.reader.XmlElementReader;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public class ViewProviderImpl implements ViewProvider {

    @Override
    public @NotNull <T extends View> Element viewFor(
            @NotNull Context context,
            @NotNull Class<? extends T> viewClass, @NotNull T view,
            @NotNull Runnable mount, @NotNull Runnable unmount) {

        if (!(context instanceof RenderableContext renderContext)) {
            throw new IllegalArgumentException("Context must be a RenderableContext");
        }

        var viewFile = viewClass.getResource(String.format("/%s.xml", viewClass.getName().replace(".", "/")));
        Check.notNull(viewFile, "View file not found: " + viewClass.getName() + ".xml");

        var rootElement = XmlElementReader.load(renderContext, viewFile.toString(), true);
        wireOutlets(viewClass, view, rootElement);
        wireActions(viewClass, view, rootElement);

        return rootElement;
    }

    private <T extends View> void wireOutlets(@NotNull Class<? extends T> viewClass, @NotNull T view, @NotNull BaseElement root) {
        try {
            for (var field : viewClass.getDeclaredFields()) {
                var outlet = field.getAnnotation(Outlet.class);
                if (outlet == null) continue;

                var name = outlet.value();
                var element = root.findById(name);
                Check.notNull(element, "Outlet not found: " + name);

                field.setAccessible(true); // NOSONAR
                if (field.getType().isAssignableFrom(element.getClass())) {
                    field.set(view, element); // NOSONAR
                } else if (element instanceof ViewContainer viewContainer &&
                        field.getType().isAssignableFrom(viewContainer.getAssociatedView().getClass())) {
                    field.set(view, viewContainer.getAssociatedView()); // NOSONAR
                } else {
                    throw new RuntimeException("Outlet type mismatch: " + name);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends View> void wireActions(@NotNull Class<? extends T> viewClass, @NotNull T view, @NotNull BaseElement root) {
        for (var method : viewClass.getDeclaredMethods()) {
            var action = method.getAnnotation(Action.class);
            if (action == null) continue;

            var name = action.value();
            var element = root.findById(name);
            Check.notNull(element, "Action not found: " + name);

            element.wireAction(view, method);
        }
    }

}
