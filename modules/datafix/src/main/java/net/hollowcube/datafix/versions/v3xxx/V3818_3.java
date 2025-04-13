package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3818_3 extends DataVersion {
    public V3818_3() {
        super(3818); // todo what is id

        // todo these paths are quite gross, should refactor.
        addReference(DataType.DATA_COMPONENTS, field -> field
                        .list("minecraft:bees.entity_data", DataType.ENTITY_TREE)
                        .single("minecraft:block_entity_data", DataType.BLOCK_ENTITY)
                        .list("minecraft:bundle_contents", DataType.ITEM_STACK)
//                .single("minecraft:can_break", DataType.) // TODO
//                .single("minecraft:can_place_on", DataType.) // TODO
                        .list("minecraft:charged_projectiles", DataType.ITEM_STACK)
                        .list("minecraft:container.item", DataType.ITEM_STACK)
                        .single("minecraft:entity_data", DataType.ENTITY_TREE)
                        .single("minecraft:pot_decorations", DataType.ITEM_NAME)
                        .single("minecraft:food.using_converts_to", DataType.ITEM_STACK)
                        .single("minecraft:custom_name", DataType.TEXT_COMPONENT)
                        .single("minecraft:item_name", DataType.TEXT_COMPONENT)
                        .list("minecraft:lore", DataType.TEXT_COMPONENT)
//                .single("minecraft:written_book_content.pages.", DataType.) // TODO
        );
    }

}
