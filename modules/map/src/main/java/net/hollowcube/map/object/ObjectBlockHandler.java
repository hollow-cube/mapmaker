package net.hollowcube.map.object;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public interface ObjectBlockHandler extends BlockHandler {

    @NotNull ObjectType objectType();

    @Override
    default @NotNull NamespaceID getNamespaceId() {
        return objectType().namespaceId();
    }

}
