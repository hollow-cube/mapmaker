package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4301 extends DataVersion {
    public V4301() {
        super(4301);

        // TODO seems gross that ENTITY_EQUIPMENT the schema still has the equipment fields.
        //  maybe its worth doing it instead where it just becomes the content.
        //  in that vein, do we actually need to register it before this moment? the
        //  fix here could easily just be a fix to entity that creates the equipment field.
        addReference(DataTypes.ENTITY_EQUIPMENT, field -> field
                .single("equipment.mainhand", DataTypes.ITEM_STACK)
                .single("equipment.offhand", DataTypes.ITEM_STACK)
                .single("equipment.feet", DataTypes.ITEM_STACK)
                .single("equipment.legs", DataTypes.ITEM_STACK)
                .single("equipment.chest", DataTypes.ITEM_STACK)
                .single("equipment.head", DataTypes.ITEM_STACK)
                .single("equipment.body", DataTypes.ITEM_STACK)
                .single("equipment.saddle", DataTypes.ITEM_STACK));

        addFix(DataTypes.ENTITY_EQUIPMENT, V4301::fixEntityEquipmentFormat);
    }

    private static Value fixEntityEquipmentFormat(Value entity) {
        var armorItems = entity.remove("ArmorItems");
        var handItems = entity.remove("HandItems");
        var feet = handItems.get(0);
        var legs = handItems.get(1);
        var chest = handItems.get(2);
        var head = handItems.get(3);
        var mainhand = armorItems.get(0);
        var offhand = armorItems.get(1);
        var body = entity.remove("body_armor_item");
        var saddle = entity.remove("saddle");

        var equipment = Value.emptyMap();
        if (isNotEmpty(feet)) equipment.put("feet", feet);
        if (isNotEmpty(legs)) equipment.put("legs", legs);
        if (isNotEmpty(chest)) equipment.put("chest", chest);
        if (isNotEmpty(head)) equipment.put("head", head);
        if (isNotEmpty(mainhand)) equipment.put("mainhand", mainhand);
        if (isNotEmpty(offhand)) equipment.put("offhand", offhand);
        if (isNotEmpty(body)) equipment.put("body", body);
        if (isNotEmpty(saddle)) equipment.put("saddle", saddle);
        if (equipment.size(0) > 0)
            entity.put("equipment", equipment);

        return null;
    }

    private static boolean isNotEmpty(Value maybeItem) {
        return maybeItem != null && maybeItem.getValue("id") != null;
    }

}
