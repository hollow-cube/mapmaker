package net.hollowcube.compat.axiom.properties;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record PropertyCategory(String name, boolean localized) {

    public static final NetworkBuffer.Type<PropertyCategory> SERIALIZER = NetworkBufferTemplate.template(
        NetworkBuffer.STRING, PropertyCategory::name,
        NetworkBuffer.BOOLEAN, PropertyCategory::localized,
        PropertyCategory::new
    );
}
