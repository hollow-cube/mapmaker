package net.hollowcube.map.feature;

import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A feature of a map. Implementations are singletons which are loaded using SPI.
 *
 *
 */
public interface MapFeature {


    //todo add some method which indicates whether the feature is enabled on a map or not.
    // similar to the kubernetes label/selector system.
    // eg CheckpointFeature#getLabels() -> mapmaker:feature/checkpoint, mapmaker:completable
    // above doesnt make much sense, eg how would we indicate it is active for maps which are not completable.

    @NotNull EventNode<InstanceEvent> eventNode();

}
