package net.hollowcube.canvas.internal.section.internal;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import net.hollowcube.canvas.internal.section.BaseElement;
import net.hollowcube.canvas.internal.section.XmlElementReader;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public class ViewProviderImpl implements ViewProvider {
    public static ViewProvider INSTANCE = new ViewProviderImpl();

    @Override
    public @NotNull <T extends View> Element viewFor(
            @NotNull Context context,
            @NotNull Class<? extends T> viewClass, @NotNull T view,
            @NotNull Runnable mount, @NotNull Runnable unmount) {

        var viewFile = viewClass.getResource(String.format("/%s.xml", viewClass.getName().replace(".", "/")));
        Check.notNull(viewFile, "View file not found: " + getClass().getName() + ".xml");

        //todo cache/clone has a lot of issues right now
        var rootElement = XmlElementReader.load(viewFile.toString(), false);
        rootElement.setAssociatedView(view, mount); //todo handle unmount
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

                field.setAccessible(true);

                var associatedView = ((BaseElement) element).getAssociatedView();
                if (field.getType().isAssignableFrom(element.getClass())) {
                    field.set(view, element);
                } else if (associatedView != null && field.getType().isAssignableFrom(associatedView.getClass())) {
                    field.set(view, associatedView);
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

            if (element instanceof BaseElement baseElement) {
                baseElement.wireAction(view, method);
            } else {
                throw new RuntimeException("todo");
            }
        }
    }
}
