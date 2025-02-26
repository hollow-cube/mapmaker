package net.hollowcube.terraform.compat.axiom;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.axiom.properties.PropertyCategory;
import net.hollowcube.compat.axiom.properties.PropertyDispatcher;
import net.hollowcube.compat.axiom.properties.registry.AxiomPropertyProvider;
import net.hollowcube.compat.axiom.properties.registry.PropertyRegistrar;
import net.hollowcube.compat.axiom.properties.types.WidgetType;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

@AutoService(AxiomPropertyProvider.class)
public class TestAxiomProperties implements AxiomPropertyProvider {

    @Override
    public void registerProperties(@NotNull PropertyRegistrar registrar) {
        registrar.register(
                new PropertyCategory("test", false),
                NamespaceID.from("test", "test"),
                "Test",
                false,
                WidgetType.Checkbox(),
                true,
                PropertyDispatcher.INSTANCE
        );
    }
}
