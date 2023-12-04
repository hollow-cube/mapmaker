package net.hollowcube.map.block.handler;

import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

public class ConduitBlockHandler implements BlockHandler {

    public static final NamespaceID ID = NamespaceID.from("minecraft:conduit");
    public static final ConduitBlockHandler INSTANCE = new ConduitBlockHandler();

    private ConduitBlockHandler() {
    }

    @Override
    public @NotNull NamespaceID getNamespaceId() {
        return ID;
    }

}
