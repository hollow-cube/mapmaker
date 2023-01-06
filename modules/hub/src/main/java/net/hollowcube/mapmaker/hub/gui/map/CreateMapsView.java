package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.mapmaker.hub.gui.common.InfoButton;
import net.hollowcube.mapmaker.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class CreateMapsView extends ParentSection {
    public CreateMapsView() {
        super(9, 3);

        // Standard
//        add(0, 2, new BackOrCloseButton());
        add(8, 0, new InfoButton("gui.create_maps.info", false));

        // Map selector
        var temp = MinecraftServer.getConnectionManager().getPlayer("notmattw").getTag(PlayerData.DATA);
        add(0, 1, new MapSlotsView(temp));

        // Other buttons
        //todo

    }

    @Override
    protected void mount() {
        super.mount();

        find(RootSection.class).setTitle(buildTitle());
    }

    private @NotNull Component buildTitle() {
        return Component.text("\uF808\uEff2", NamedTextColor.WHITE);
    }
}
