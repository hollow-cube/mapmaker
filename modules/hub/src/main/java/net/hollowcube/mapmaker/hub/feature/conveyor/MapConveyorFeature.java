package net.hollowcube.mapmaker.hub.feature.conveyor;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import org.jetbrains.annotations.NotNull;

@AutoService(HubFeature.class)
public class MapConveyorFeature implements HubFeature {

    @Override
    public void init(@NotNull HubServer hub) {

//        FutureUtil.submitVirtual(new LeftMapAnimator(hub.instance())::loop);
//        FutureUtil.submitVirtual(new CenterMapAnimator(hub.instance())::loop);
//        FutureUtil.submitVirtual(new RightMapAnimator(hub.instance())::loop);
    }


}
