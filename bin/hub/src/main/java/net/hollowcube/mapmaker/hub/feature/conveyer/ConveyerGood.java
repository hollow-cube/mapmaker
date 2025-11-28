package net.hollowcube.mapmaker.hub.feature.conveyer;

import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.conveyer.parts.ConveyerEnd;
import net.hollowcube.mapmaker.hub.feature.conveyer.parts.ConveyerStart;

public record ConveyerGood(
        NpcItemModel good,
        ConveyerStart source,
        ConveyerEnd destination
) {
}
