package net.hollowcube.mapmaker.hub.feature.leaderboard;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Leaderboard2 {
    private static final double CENTER_BIAS = 0.5; // Higher value moves the text closer to together. Used for manual horizontal alignment
    private static final double SCREEN_WIDTH = 11; // The width of the inner part of the screen model
    private static final double MODEL_SCALE = 16;

    private final NpcItemModel screenModel = new NpcItemModel();
    private final LeaderboardText left;
    private final LeaderboardText right;

    private LeaderboardData leftData = new LeaderboardData(List.of(
            new LeaderboardData.Entry("notmattw", 150, 1),
            new LeaderboardData.Entry("Ossipago1", 49, 2),
            new LeaderboardData.Entry("Ontal", 45, 3),
            new LeaderboardData.Entry("HammSamichz", 44, 4),
            new LeaderboardData.Entry("SethPRG", 43, 5),
            new LeaderboardData.Entry("Nixotica", 40, 6),
            new LeaderboardData.Entry("ArcaneWarrior", 32, 7),
            new LeaderboardData.Entry("Kha0x", 23, 8),
            new LeaderboardData.Entry("YouAreRexist", 15, 9),
            new LeaderboardData.Entry("MMMMMMMMMMMMMMM", 999, 10)
    ), null);

    public Leaderboard2(double screenAngle) {
        screenModel.setModel(Material.STICK, 4);
        screenModel.getEntityMeta().setScale(new Vec(MODEL_SCALE));
        screenModel.getEntityMeta().setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(screenAngle)).into());

        // X: The text is centered in the displays so we need to put it at 1/4 of the width of the screen to be aligned with the other one.
        //    however it looked kinda strange so i did a completely arbitrary subtraction of 0.5 to bias the text towards the center.
        left = new LeaderboardText(-(SCREEN_WIDTH - CENTER_BIAS) / 4, screenAngle);
        right = new LeaderboardText((SCREEN_WIDTH - CENTER_BIAS) / 4, screenAngle);

        left.setData(leftData);
        right.setData(leftData);
    }

    public void setInstance(@NotNull Instance instance, @NotNull Pos pos) {
        screenModel.setInstance(instance, pos).join();
        left.setInstance(instance, pos).join();
        right.setInstance(instance, pos).join();
    }
}
