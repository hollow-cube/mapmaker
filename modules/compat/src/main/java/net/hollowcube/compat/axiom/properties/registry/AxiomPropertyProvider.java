package net.hollowcube.compat.axiom.properties.registry;

import org.jetbrains.annotations.NotNull;

public interface AxiomPropertyProvider {

    void registerProperties(@NotNull PropertyRegistrar registrar);
}
