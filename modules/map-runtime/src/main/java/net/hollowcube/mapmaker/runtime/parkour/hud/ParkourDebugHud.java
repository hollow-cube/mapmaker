package net.hollowcube.mapmaker.runtime.parkour.hud;

import net.hollowcube.common.util.FontUIBuilder;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld2;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.minestom.server.entity.Player;

public class ParkourDebugHud implements ActionBar.Provider {
    public static final ParkourDebugHud INSTANCE = new ParkourDebugHud();

    private ParkourDebugHud() {
    }

    @Override
    public void provide(Player player, FontUIBuilder builder) {
        var world = ParkourMapWorld2.forPlayer(player);
        if (world == null) return;

        if (!(world.getPlayerState(player) instanceof ParkourState.Playing(var saveState, var _)))
            return;

        var sliced = slice(saveState.state(PlayState.class).toString(false));
        builder.offset(-FontUtil.measureText(sliced) / 2)
                .append("line_0", sliced);
        builder.offset(-FontUtil.measureText(sliced) / 2);

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
