package net.hollowcube.canvas.internal.standalone.provider;

import com.google.auto.service.AutoService;
import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.canvas.internal.ControllerFactory;
import org.jetbrains.annotations.NotNull;

@AutoService(ControllerFactory.class)
public class ControllerFactoryImpl implements ControllerFactory {
    @Override
    public @NotNull Controller create() {
        return new ControllerImpl();
    }
}
