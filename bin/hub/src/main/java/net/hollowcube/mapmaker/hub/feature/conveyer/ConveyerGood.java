package net.hollowcube.mapmaker.hub.feature.conveyer;

import net.hollowcube.mapmaker.hub.feature.conveyer.parts.ConveyerEnd;
import net.hollowcube.mapmaker.hub.feature.conveyer.parts.ConveyerStart;

public final class ConveyerGood {
    private final ConveyerItemModel good;
    private final ConveyerStart source;
    private ConveyerEnd destination;

    public ConveyerGood(
            ConveyerItemModel good,
            ConveyerStart source,
            ConveyerEnd destination
    ) {
        this.good = good;
        this.source = source;
        this.destination = destination;
    }

    public ConveyerItemModel good() {
        return good;
    }

    public ConveyerStart source() {
        return source;
    }

    public ConveyerEnd destination() {
        return destination;
    }

    public void withDestination(ConveyerEnd end) {
        this.destination = end;
    }

}
