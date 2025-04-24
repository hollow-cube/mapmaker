package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3818_3 extends DataVersion {
    public V3818_3() {
        super(3818, 3);

        // todo these paths are quite gross, should refactor.
        addReference(DataTypes.DATA_COMPONENTS, field -> field
                        .list("minecraft:bees.entity_data", DataTypes.ENTITY)
                        .single("minecraft:block_entity_data", DataTypes.BLOCK_ENTITY)
                        .list("minecraft:bundle_contents", DataTypes.ITEM_STACK)
//                .single("minecraft:can_break", DataType.) // TODO
//                .single("minecraft:can_place_on", DataType.) // TODO
                        .list("minecraft:charged_projectiles", DataTypes.ITEM_STACK)
                        .list("minecraft:container.item", DataTypes.ITEM_STACK)
                        .single("minecraft:entity_data", DataTypes.ENTITY)
                        .single("minecraft:pot_decorations", DataTypes.ITEM_NAME)
                        .single("minecraft:food.using_converts_to", DataTypes.ITEM_STACK)
                        .single("minecraft:custom_name", DataTypes.TEXT_COMPONENT)
                        .single("minecraft:item_name", DataTypes.TEXT_COMPONENT)
                        .list("minecraft:lore", DataTypes.TEXT_COMPONENT)
//                .single("minecraft:written_book_content.pages.", DataType.) // TODO
        );
    }

}
