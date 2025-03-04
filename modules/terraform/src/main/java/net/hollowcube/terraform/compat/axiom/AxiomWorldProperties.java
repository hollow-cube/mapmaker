package net.hollowcube.terraform.compat.axiom;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.axiom.properties.registry.AxiomPropertyProvider;
import net.hollowcube.compat.axiom.properties.registry.PropertyRegistrar;
import org.jetbrains.annotations.NotNull;

@AutoService(AxiomPropertyProvider.class)
public class AxiomWorldProperties implements AxiomPropertyProvider {

    @Override
    public void registerProperties(@NotNull PropertyRegistrar registrar) {
        // TODO setup properties
    }
}
