package net.hollowcube.canvas.internal.standalone.provider;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.ViewElement;
import net.hollowcube.canvas.annotation.*;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import net.hollowcube.canvas.internal.standalone.BaseElement;
import net.hollowcube.canvas.internal.standalone.ViewContainer;
import net.hollowcube.canvas.internal.standalone.context.RenderableContext;
import net.hollowcube.canvas.internal.standalone.reader.XmlElementReader;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public class ViewProviderImpl implements ViewProvider {

    @Override
    public @NotNull <T extends View> ViewElement viewFor(
            @NotNull Context context,
            @NotNull Class<? extends T> viewClass, @NotNull T view,
            @NotNull Runnable mount, @NotNull Runnable unmount) {

        if (!(context instanceof RenderableContext renderContext)) {
            throw new IllegalArgumentException("Context must be a RenderableContext");
        }

        var viewFile = viewClass.getResource(String.format("/%s.xml", viewClass.getName().replace(".", "/")));
        Check.notNull(viewFile, "View file not found: " + viewClass.getName() + ".xml");

        var rootElement = XmlElementReader.load(renderContext, viewFile.toString(), true);
        rootElement.setAssociatedView(view);
        wireContextObjects(viewClass, view, renderContext);
        wireOutlets(viewClass, view, rootElement);
        wireActions(viewClass, view, rootElement);
        wireSignals(viewClass, view, rootElement);

        return rootElement;
    }

    private <T extends View> void wireContextObjects(@NotNull Class<? extends T> viewClass, @NotNull T view, @NotNull RenderableContext context) {
        if (!viewClass.getSuperclass().equals(View.class))
            wireContextObjects((Class<? extends View>) viewClass.getSuperclass(), view, context);

        try {
            for (var field : viewClass.getDeclaredFields()) {
                var annotation = field.getAnnotation(ContextObject.class);
                if (annotation == null) continue;

                var name = field.getName();
                if (!annotation.value().isBlank())
                    name = annotation.value();

                var contextObject = context.contextObjects().get(name.toLowerCase(Locale.ROOT));
                Check.notNull(contextObject, "Context object not found: " + name);

                field.setAccessible(true); // NOSONAR
                if (field.getType().isAssignableFrom(contextObject.getClass())) {
                    field.set(view, contextObject); // NOSONAR
                } else {
                    throw new RuntimeException("Context object type mismatch: " + name);
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends View> void wireOutlets(@NotNull Class<? extends T> viewClass, @NotNull T view, @NotNull BaseElement root) {
        if (!viewClass.getSuperclass().equals(View.class))
            wireOutlets((Class<? extends View>) viewClass.getSuperclass(), view, root);

        try {
            for (var field : viewClass.getDeclaredFields()) {
                var outlet = field.getAnnotation(Outlet.class);
                if (outlet != null) {
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

                var outletGroup = field.getAnnotation(OutletGroup.class);
                if (outletGroup != null) {
                    var pattern = Pattern.compile(outletGroup.value());
                    var elements = new ArrayList<Element>();
                    root.collectById(pattern.asMatchPredicate(), elements);

                    Check.argCondition(!field.getType().isArray(), "OutletGroup must be an array type: " + field.getType());
                    field.setAccessible(true); // NOSONAR

                    var arrayType = field.getType().getComponentType();
                    var array = (Object[]) java.lang.reflect.Array.newInstance(arrayType, elements.size());
                    for (int i = 0; i < array.length; i++) {
                        var element = elements.get(i);
                        if (arrayType.isAssignableFrom(element.getClass())) {
                            array[i] = element;
                        } else if (element instanceof ViewContainer viewContainer &&
                                arrayType.isAssignableFrom(viewContainer.getAssociatedView().getClass())) {
                            array[i] = viewContainer.getAssociatedView(); // NOSONAR
                        } else {
                            throw new RuntimeException("Outlet type mismatch: " + element);
                        }
                    }
                    field.set(view, array);
                }

            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends View> void wireActions(@NotNull Class<? extends T> viewClass, @NotNull T view, @NotNull BaseElement root) {
        if (!viewClass.getSuperclass().equals(View.class))
            wireActions((Class<? extends View>) viewClass.getSuperclass(), view, root);

        for (var method : viewClass.getDeclaredMethods()) {
            var action = method.getAnnotation(Action.class);
            var group = method.getAnnotation(ActionGroup.class);

            Check.stateCondition(action != null && group != null, "Method cannot have both Action and ActionGroup annotations");

            if (action != null) {
                var element = root.findById(action.value());
                Check.notNull(element, "Action not found: " + action.value());

                element.wireAction(view, method, new Action.Descriptor(action.value(), action.async()));
            } else if (group != null) {
                var pattern = Pattern.compile(group.value());
                var elements = new ArrayList<Element>();
                root.collectById(pattern.asMatchPredicate(), elements);

                for (var element : elements) {
                    if (!(element instanceof BaseElement base)) continue;
                    base.wireAction(view, method, new Action.Descriptor(Objects.requireNonNull(element.id()), group.async()));
                }
            }
        }
    }

    private <T extends View> void wireSignals(@NotNull Class<? extends T> viewClass, @NotNull T view, @NotNull ViewContainer root) {
        if (!viewClass.getSuperclass().equals(View.class))
            wireSignals((Class<? extends View>) viewClass.getSuperclass(), view, root);

        for (var method : viewClass.getDeclaredMethods()) {
            var action = method.getAnnotation(Signal.class);
            if (action == null) continue;

            var name = action.value();
            root.addSignal(name.toLowerCase(Locale.ROOT), args -> {
                try {
                    method.setAccessible(true);
                    method.invoke(view, args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

}
