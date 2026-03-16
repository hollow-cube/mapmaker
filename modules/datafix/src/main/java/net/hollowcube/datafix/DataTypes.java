package net.hollowcube.datafix;

import net.kyori.adventure.key.Key;

import static net.hollowcube.datafix.DataType.dataType;
import static net.hollowcube.datafix.DataType.idMappedDataType;

public interface DataTypes {

    DataType BLOCK_NAME = dataType(Key.key("block_name"));
    DataType BLOCK_STATE = dataType(Key.key("block_state"));
    DataType FLAT_BLOCK_STATE = dataType(Key.key("flat_block_state"));
    DataType.IdMapped BLOCK_ENTITY = idMappedDataType(Key.key("block_entity"));

    DataType ITEM_NAME = dataType(Key.key("item_name"));
    DataType DATA_COMPONENTS = dataType(Key.key("data_components"));
    DataType.IdMapped ITEM_STACK = idMappedDataType(Key.key("item_stack"));

    DataType ENTITY_NAME = dataType(Key.key("entity_name"));
    DataType ENTITY_EQUIPMENT = dataType(Key.key("entity_equipment"));
    DataType.IdMapped ENTITY = idMappedDataType(Key.key("entity"));

    DataType BIOME_NAME = dataType(Key.key("biome_name"));
    DataType TEXT_COMPONENT = dataType(Key.key("text_component"));

    // TODO should go back and support GAME_EVENT since they exist inside of entities and block

}
