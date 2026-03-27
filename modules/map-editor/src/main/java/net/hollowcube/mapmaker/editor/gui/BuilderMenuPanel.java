package net.hollowcube.mapmaker.editor.gui;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.*;
import net.kyori.adventure.text.Component;
import net.minestom.server.inventory.InventoryType;

import java.util.Arrays;
import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.backOrClose;
import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class BuilderMenuPanel extends Panel {

    private final Text title;
    private final Pagination<BuilderMenuTabs> pagination;

    public BuilderMenuPanel(ServerBridge bridge) {
        super(InventoryType.CHEST_4_ROW, 9, 7);

        background("generic2/containers/extended/7x1x1", -10, -31);
        add(0, 0, title("Builder Menu"));

        add(0, 0, backOrClose());
        this.title = add(1, 0, new Text(7, 1, "")
            .background("generic2/btn/default/7_1")
            .align(Text.CENTER, 4)
        );
        add(8, 0, new Button(1, 1)
            .background("generic2/btn/danger/1_1")
            .sprite("icon2/1_1/running_out_door", 1, 1)
            .translationKey("gui.builder_menu.save_and_exit")
            .onLeftClickAsync(() -> bridge.joinHub(this.host.player()))
        );

        this.pagination = add(1, 2, new Pagination<>(7, 1));
        this.pagination.fetch(this::getItems);

        add(0, 4, new Text(9, 1, FontUtil.rewrite("small", "tabs")).align(Text.CENTER, 6));

        var tabs = add(3, 5, new RadioSelect<BuilderMenuTabs>(3, 1));
        tabs.onChange(this::setTab);

        for (var tab : BuilderMenuTabs.values()) {
            tabs.addOption(tab, (button, selected) -> {
                button.background(selected ? "generic2/btn/selected/1_1ex" : "generic2/btn/default/1_1ex");
                button.sprite(tab.icon(), 1, selected ? 3 : 1);
                button.translationKey(tab.translation());
                button.lorePostfix(selected ? List.of() : List.of(Component.translatable("gui.builder_menu.tab.select")));
            });
        }

        tabs.setSelected(BuilderMenuTabs.CUSTOM_BLOCKS);
    }

    @Override
    protected void mount(InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);

        this.setTab(BuilderMenuTabs.CUSTOM_BLOCKS);
    }

    private void setTab(BuilderMenuTabs tab) {
        this.title.text(LanguageProviderV2.translateToPlain(tab.translation() + ".name"));
        this.pagination.reset(tab);
    }

    private List<? extends Element> getItems(BuilderMenuTabs tab, int page, int pageSize) {
        return Arrays.stream(tab.items())
            .map(item -> {
                var button = new Button(1, 1);
                button.from(item.icon());
                button.translationKey(item.translation());
                button.onLeftClick(() -> item.give(this.host.player()));
                return button;
            })
            .toList();
    }
}
