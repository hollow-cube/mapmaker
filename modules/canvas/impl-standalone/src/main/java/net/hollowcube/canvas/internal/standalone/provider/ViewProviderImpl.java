package net.hollowcube.canvas.internal.standalone.provider;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.canvas.internal.ViewProvider;
import net.hollowcube.canvas.internal.standalone.reader.XmlElementReader;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

public class ViewProviderImpl implements ViewProvider {

    @Override
    public @NotNull <T extends View> Element viewFor(
            @NotNull Context context,
            @NotNull Class<? extends T> viewClass, @NotNull T view,
            @NotNull Runnable mount, @NotNull Runnable unmount) {

//        var viewFile = viewClass.getResource(String.format("/%s.xml", viewClass.getName().replace(".", "/")));
        var viewFile = "/Users/matt/dev/projects/mmo/mapmaker/modules/canvas/impl-standalone/src/test/resources/net.hollowcube.canvas.demo/Counter.xml";
        Check.notNull(viewFile, "View file not found: " + viewClass.getName() + ".xml");

        var rootElement = XmlElementReader.load(viewFile.toString(), true);
        return rootElement;
    }

}
