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

public class Leaderboard1 {
    private static final double MODEL_SCALE = 16;

    private final NpcItemModel screenModel = new NpcItemModel();
    private final LeaderboardText text;

    private LeaderboardData data = new LeaderboardData(List.of(
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

    public Leaderboard1(double screenAngle) {
        screenModel.setModel(Material.STICK, 8);
        screenModel.getEntityMeta().setScale(new Vec(MODEL_SCALE));
        screenModel.getEntityMeta().setLeftRotation(new Quaternion(new Vec(1, 0, 0).normalize(),
                Math.toRadians(screenAngle)).into());

        text = new LeaderboardText(0, screenAngle);
        text.setData(data);
    }

    public void setInstance(@NotNull Instance instance, @NotNull Pos pos) {
        screenModel.setInstance(instance, pos).join();
        text.setInstance(instance, pos).join();
    }
}
