package net.hollowcube.mapmaker.map.gui.buildermenu;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuilderMenuView extends View {
    private static final String[] TAB_NAMES = new String[]{"Custom Blocks", "Build Tools", "Custom Items"};

    private @ContextObject("bridge") ServerBridge bridge;

    private @Outlet("title") Text titleText;
    private @Outlet("tab_name") Text tabNameText;
    private @Outlet("tab_content") Switch tabContentSwitch;
    private @OutletGroup("tab_.+_switch") Switch[] tabSwitches;

    private @Outlet("custom_blocks") BuilderMenuTab customBlocksTab;
    private @Outlet("build_tools") BuilderMenuTab buildToolsTab;
    private @Outlet("custom_items") BuilderMenuTab customItemsTab;

    private int selectedTab = -1;

    public BuilderMenuView(@NotNull Context context) {
        super(context);

        titleText.setText("Builder Menu");
        selectTab(0);

        customBlocksTab.setItems(BuilderMenuTabItems.CUSTOM_BLOCKS);
        buildToolsTab.setItems(BuilderMenuTabItems.BUILD_TOOLS);
        customItemsTab.setItems(BuilderMenuTabItems.CUSTOM_ITEMS);
    }

    private void selectTab(int ordinal) {
        if (selectedTab == ordinal) return;
        if (selectedTab >= 0) tabSwitches[selectedTab].setOption(0);

        tabNameText.setText(TAB_NAMES[ordinal]);
        tabNameText.setArgs(Component.text(TAB_NAMES[ordinal]));
        tabContentSwitch.setOption(ordinal);
        tabSwitches[ordinal].setOption(1);
        selectedTab = ordinal;
    }

    @Action("tab_custom_blocks_off")
    private void selectCustomBlocks() {
        selectTab(0);
    }

    @Action("tab_build_tools_off")
    private void selectBuildTools() {
        selectTab(1);
    }

    @Action("tab_custom_items_off")
    private void selectCustomItems() {
        selectTab(2);
    }

    @Action("save_and_exit")
    private void saveAndExit(@NotNull Player player) {
        player.closeInventory();
        var world = MapWorld.forPlayerOptional(player);
        if (world != null) world.removePlayer(player);
        bridge.joinHub(player);
    }

}
