package net.hollowcube.datafix;

import org.jetbrains.annotations.NotNull;

public interface DataType {

    @NotNull DataType.IdMapped BLOCK_ENTITY = makeId("block_entity");
    @NotNull DataType.IdMapped ITEM_STACK = makeId("item_stack");
    @NotNull DataType BLOCK_STATE = make("block_state");
    @NotNull DataType FLAT_BLOCK_STATE = make("flat_block_state");
    @NotNull DataType.IdMapped DATA_COMPONENTS = makeId("data_components");
    @NotNull DataType TEXT_COMPONENT = make("text_component");
    @NotNull DataType ENTITY_EQUIPMENT = make("entity_equipment");
    @NotNull DataType ENTITY_NAME = make("entity_name");
    @NotNull DataType ENTITY_TREE = make("entity_tree");
    @NotNull DataType.IdMapped ENTITY = makeId("entity");
    @NotNull DataType BLOCK_NAME = make("block_name");
    @NotNull DataType ITEM_NAME = make("item_name");

    interface IdMapped extends DataType {

    }

    private static @NotNull DataType make(@NotNull String name) {
        return new DataTypeImpl(name);
    }

    private static @NotNull IdMapped makeId(@NotNull String name) {
        return new DataTypeImpl.IdMapped(name);
    }

}
