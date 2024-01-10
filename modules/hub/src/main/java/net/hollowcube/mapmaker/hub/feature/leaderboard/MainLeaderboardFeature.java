package net.hollowcube.mapmaker.hub.feature.leaderboard;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoService(HubFeature.class)
public class MainLeaderboardFeature implements HubFeature {
    private static final Logger logger = LoggerFactory.getLogger(MainLeaderboardFeature.class);

    // left/right are from the perspective of looking towards the leaderboards
    private final Leaderboard2 leaderboardLeft = new Leaderboard2(10);
    private final Leaderboard2 leaderboardRight = new Leaderboard2(10);

    private final Leaderboard1 testlb = new Leaderboard1(0);
    private final Leaderboard1 testlb2 = new Leaderboard1(0);

    @Override
    public void init(@NotNull HubServer hub) {
        leaderboardLeft.setInstance(hub.instance(), new Pos(6, 39, -22.5, 90, 0));
        leaderboardRight.setInstance(hub.instance(), new Pos(6, 39, 23.5, 90, 0));


        testlb.setInstance(hub.instance(), new Pos(-25.5, 41, 53.5, 90 + 45, 0));
        testlb2.setInstance(hub.instance(), new Pos(-49.5, 41, 53.5, 90 + 45 + 90, 0));
    }
}
