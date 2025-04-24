package net.hollowcube.datafix;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.datafix.DataType.dataType;
import static net.hollowcube.datafix.DataType.idMappedDataType;

public interface DataTypes {

    @NotNull DataType BLOCK_NAME = dataType(Key.key("block_name"));
    @NotNull DataType BLOCK_STATE = dataType(Key.key("block_state"));
    @NotNull DataType FLAT_BLOCK_STATE = dataType(Key.key("flat_block_state"));
    @NotNull DataType.IdMapped BLOCK_ENTITY = idMappedDataType(Key.key("block_entity"));

    @NotNull DataType ITEM_NAME = dataType(Key.key("item_name"));
    @NotNull DataType DATA_COMPONENTS = dataType(Key.key("data_components"));
    @NotNull DataType.IdMapped ITEM_STACK = idMappedDataType(Key.key("item_stack"));

    @NotNull DataType ENTITY_NAME = dataType(Key.key("entity_name"));
    @NotNull DataType ENTITY_EQUIPMENT = dataType(Key.key("entity_equipment"));
    @NotNull DataType.IdMapped ENTITY = idMappedDataType(Key.key("entity"));

    @NotNull DataType BIOME_NAME = dataType(Key.key("biome_name"));
    @NotNull DataType TEXT_COMPONENT = dataType(Key.key("text_component"));

    // TODO should go back and support GAME_EVENT since they exist inside of entities and block

}
