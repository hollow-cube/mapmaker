package net.hollowcube.mapmaker.hub.feature.conveyer;

import net.hollowcube.mapmaker.hub.feature.conveyer.parts.ConveyerEnd;
import net.hollowcube.mapmaker.hub.feature.conveyer.parts.ConveyerStart;

public record ConveyerGood(
        ConveyerItemModel good,
        ConveyerStart source,
        ConveyerEnd destination
) {
}
