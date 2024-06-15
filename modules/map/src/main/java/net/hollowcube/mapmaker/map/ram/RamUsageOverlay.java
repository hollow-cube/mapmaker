package net.hollowcube.mapmaker.map.ram;

import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.FontUIBuilder;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RamUsageOverlay implements ActionBar.Provider {

    public static final RamUsageOverlay INSTANCE = new RamUsageOverlay();

    private RamUsageOverlay() {
    }

    @Override
    public void provide(@NotNull Player player, @NotNull FontUIBuilder builder) {
//        MapWorld world = MapWorld.forPlayerOptional(player);
//        if (!(world instanceof EditingMapWorld editingWorld)) return;
//
//        int memoryWidth = FontUtil.measureText("small", "memory");
//        var usageText = ": " + editingWorld.memoryUsage() + "/" + (editingWorld.maxMemoryUsage() == -1 ? "∞" : editingWorld.maxMemoryUsage());
//        int restWidth = FontUtil.measureText(usageText);
//        builder.append("ᴍᴇᴍᴏʀʏ" + usageText, memoryWidth + restWidth);
    }
}
