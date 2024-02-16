package net.hollowcube.mapmaker.hub.feature.conveyor;

import com.google.auto.service.AutoService;
import com.google.inject.Inject;
import net.hollowcube.mapmaker.hub.feature.HubFeature;

@AutoService(HubFeature.class)
public class MapConveyorFeature implements HubFeature {

    @Inject
    public MapConveyorFeature() {

//        FutureUtil.submitVirtual(new LeftMapAnimator(hub.instance())::loop);
//        FutureUtil.submitVirtual(new CenterMapAnimator(hub.instance())::loop);
//        FutureUtil.submitVirtual(new RightMapAnimator(hub.instance())::loop);
    }


}
