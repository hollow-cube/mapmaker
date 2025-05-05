package net.hollowcube.mapmaker.gui.common;

import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Element;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import org.jetbrains.annotations.NotNull;

public final class ExtraPanels {

    public static Element title(@NotNull String text) {
        return new Text("", 9, 0, text).align(Text.CENTER, -23);
    }

    public static Element backOrClose() {
        return new BackOrClosePanel();
    }

    public static Element info(@NotNull String name) {
        return new Button("gui." + name + ".information", 1, 1)
                .background("generic2/btn/default/1_1")
                .sprite("generic2/btn/common/info", 4, 2);
    }

    private static class BackOrClosePanel extends Panel {

        public BackOrClosePanel() {
            super(1, 1);

            // todo support back
            add(0, 0, new Button("gui.generic.close_menu", 1, 1)
                    .background("generic2/btn/danger/1_1")
                    .sprite("generic2/btn/common/close", 4, 4)
                    .onLeftClick(player -> player.closeInventory()));
        }

    }

}
