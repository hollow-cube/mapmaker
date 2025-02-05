package net.hollowcube.mapmaker.map.feature.play.setting;

import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.FeatureProvider;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSettingFeatureProvider implements FeatureProvider {

    @Override
    public boolean initMap(@NotNull MapWorld world) {
        if (!(world instanceof PlayingMapWorld || world instanceof TestingMapWorld)) return false;
        var settings = world.map().settings();
        if (settings.getVariant() != MapVariant.PARKOUR) return false;

        world.eventNode().addChild(getEvents());

        return true;
    }

    protected abstract EventNode<InstanceEvent> getEvents();
}
