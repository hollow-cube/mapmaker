package net.hollowcube.map.feature.experimental.object;

import net.hollowcube.map.object.ObjectBlockHandler;
import net.hollowcube.map2.item.handler.BlockItemHandler;
import net.hollowcube.mapmaker.object.ObjectType;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;

public class TestObjectBlock implements ObjectBlockHandler {
    public static final ObjectType OBJECT_TYPE = ObjectType.builder("mapmaker:test_object")
            .cost(50)
            .build();

    public static final TestObjectBlock INSTANCE = new TestObjectBlock();
    public static final BlockItemHandler ITEM = new BlockItemHandler(INSTANCE, Block.COAL_BLOCK);

    private TestObjectBlock() {
    }

    @Override
    public @NotNull ObjectType objectType() {
        return OBJECT_TYPE;
    }

    @Override
    public void onPlace(@NotNull Placement placement) {
        System.out.println("placed test object");
    }
}
