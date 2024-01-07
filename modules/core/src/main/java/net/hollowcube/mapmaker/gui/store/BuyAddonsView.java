package net.hollowcube.mapmaker.gui.store;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

public class BuyAddonsView extends View {

    private @Outlet("map_size_switch") Switch mapSizeSwitch;
    private @Outlet("map_slots_switch") Switch mapSlotsSwitch;
    private @Outlet("terraform_switch") Switch terraformSwitch;
    private @Outlet("folder_switch") Switch folderSwitch;

    public BuyAddonsView(@NotNull Context context) {
        super(context);

        mapSizeSwitch.setOption(0);
        mapSlotsSwitch.setOption(0);
        terraformSwitch.setOption(0);
        folderSwitch.setOption(0);
        // TODO base the set option on what packages the user has purchased
    }

    // TODO switch on button click to see the other buttons
    private void testMapSize() {
        switch (mapSizeSwitch.getOption()) {
            case 0 -> mapSizeSwitch.setOption(1);
            case 1 -> mapSizeSwitch.setOption(2);
            case 2 -> mapSizeSwitch.setOption(3);
            case 3 -> mapSizeSwitch.setOption(0);
        }
    }
    
    private void testMapSlot() {
        switch (mapSlotsSwitch.getOption()) {
            case 0 -> mapSlotsSwitch.setOption(1);
            case 1 -> mapSlotsSwitch.setOption(2);
            case 2 -> mapSlotsSwitch.setOption(3);
            case 3 -> mapSlotsSwitch.setOption(0);
        }
    }
    
    private void testTerraform() {
        switch (terraformSwitch.getOption()) {
            case 0 -> terraformSwitch.setOption(1);
            case 1 -> terraformSwitch.setOption(2);
            case 2 -> terraformSwitch.setOption(0);
        }
    }
    
    private void testFolder() {
        switch (folderSwitch.getOption()) {
            case 0 -> folderSwitch.setOption(1);
            case 1 -> folderSwitch.setOption(0);
        }
    }
}
