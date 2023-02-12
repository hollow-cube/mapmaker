package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.section.ParentSection;
import net.hollowcube.canvas.section.RootSection;
import net.hollowcube.mapmaker.hub.gui.common.InfoButton;
import net.hollowcube.mapmaker.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMapsView extends ParentSection {
    private PlayerData playerData = null;

    public CreateMapsView() {
        super(9, 3);

        // Standard
//        add(0, 2, new BackOrCloseButton());
        add(8, 0, new InfoButton("gui.create_maps.info", false));

        // Map selector added in mount

        // Other buttons
        //todo

    }

    @Override
    protected void mount() {
        super.mount();

        if (playerData == null) {
            playerData = PlayerData.fromPlayer(getContext(Player.class));
            add(0, 1, new MapSlotsView(playerData));
        }

        find(RootSection.class).setTitle(buildTitle());
    }

    private @NotNull Component buildTitle() {
        return Component.text("\uF808\uEff2", NamedTextColor.WHITE);
    }
}
