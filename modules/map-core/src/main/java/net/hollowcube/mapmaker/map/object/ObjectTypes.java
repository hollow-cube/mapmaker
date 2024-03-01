package net.hollowcube.mapmaker.map.object;

import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.object.ObjectType;

public class ObjectTypes {

    public static final ObjectType CHECKPOINT_PLATE = ObjectType.builder("mapmaker:checkpoint_plate")
            .requiredVariant(MapVariant.PARKOUR)
            .build();
    public static final ObjectType FINISH_PLATE = ObjectType.builder("mapmaker:finish_plate")
            .requiredVariant(MapVariant.PARKOUR)
            .build();

}
