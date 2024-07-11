package net.hollowcube.mapmaker.hub.feature.leaderboard;

import net.hollowcube.common.math.Quaternion;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        text.setEntriesRaw(Component.text("ᴄᴏᴍɪɴɢ ѕᴏᴏɴ...", NamedTextColor.DARK_GRAY));
//        text.setData(s -> new DisplayName(List.of(new DisplayName.Part("username", s, null))), data);
    }

    public void setInstance(@NotNull Instance instance, @NotNull Pos pos) {
        FutureUtil.getUnchecked(screenModel.setInstance(instance, pos));
        FutureUtil.getUnchecked(text.setInstance(instance, pos));
    }
}
