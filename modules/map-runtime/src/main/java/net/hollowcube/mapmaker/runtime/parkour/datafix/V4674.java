package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;

@AutoService(ExternalDataFix.class)
public class V4674 extends DataVersion implements ExternalDataFix {

    public V4674() {
        super(4674);

        addReference(HCDataTypes.PLAY_STATE, field -> field
            .list("entities", DataTypes.ENTITY));
    }

}
