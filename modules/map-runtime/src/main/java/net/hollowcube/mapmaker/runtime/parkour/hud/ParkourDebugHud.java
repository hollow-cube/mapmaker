package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.minestom.server.entity.Player;

import java.util.Objects;

public class ParkourDebugHud implements ActionBar.Provider {
    public static final ParkourDebugHud INSTANCE = new ParkourDebugHud();

    private ParkourDebugHud() {
    }

    @Override
    public int cacheKey(Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        return Objects.hash(world != null, world != null ? world.getPlayerState(player) : null);
    }

    @Override
    public void provide(Player player, FontUIBuilder builder) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return;
        var saveState = p.saveState();

        var sliced = slice(saveState.state(PlayState.class).toString(false));
        builder.offset(-FontUtil.measureText(sliced) / 2).append("line_0", sliced);
        builder.offset(-FontUtil.measureText(sliced) / 2);

        builder.pushColor(FontUtil.computeVerticalOffset(-50));
        builder.append("line_0", "Ticks=%d".formatted(saveState.getTicks()));
        builder.offset(-FontUtil.measureText("Ticks=%d".formatted(saveState.getTicks())));
        builder.popColor();

        builder.offset(-300);
        int i = 0;
        var actions = saveState.state(PlayState.class).actionData();
        for (var entry : actions.entrySet()) {
            builder.pushColor(FontUtil.computeVerticalOffset(-50 + (i++ * 9)));
            if (entry.getKey() == null) continue;
            var line = entry.getKey().key().asString().replace("mapmaker:", "") + ": " + entry.getValue();
            if (line.length() > 150)
                line = line.substring(0, 150) + "...";
            builder.append(line);
            builder.offset(-FontUtil.measureText(line));
            builder.popColor();
        }
    }

    private String slice(String str) {
        if (str.length() > 200) {
            return str.substring(0, 200) + "...";
        }
        return str;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ParkourDebugHud;
    }

    @Override
    public int hashCode() {
        return 31 * ParkourDebugHud.class.hashCode();
    }
}
