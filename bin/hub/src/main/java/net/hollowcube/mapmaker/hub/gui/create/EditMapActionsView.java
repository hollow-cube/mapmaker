package net.hollowcube.mapmaker.hub.gui.create;

import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.title;

public class EditMapActionsView extends Panel {

    public EditMapActionsView() {
        super(9, 10);

        background("create_maps2/edit/actions_container", -10, -31);
        add(0, 0, title("Edit Map"));

        add(1, 2, new Button("copy", 3, 2));

        add(5, 2, new Button("resize", 3, 2));

        add(1, 4, new Button("transfer", 3, 2));

        add(5, 4, new Button("delete", 3, 2));
    }
}
