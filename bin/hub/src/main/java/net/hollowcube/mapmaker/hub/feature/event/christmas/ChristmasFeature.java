package net.hollowcube.mapmaker.hub.feature.event.christmas;

import com.google.auto.service.AutoService;
import net.hollowcube.command.dsl.SimpleCommand;
import net.hollowcube.mapmaker.cosmetic.Hats;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.entity.NpcPlayer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.gui.event.AdventCalanderPanel;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.runtime.AbstractMapServer;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.ServerFlag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.timer.TaskSchedule;

@AutoService(HubFeature.class)
public class ChristmasFeature implements HubFeature {

    private static final Pos NPC_POS = new Pos(-37.5, 40, 47.5, 180, 10);
    private static final Pos PRESENT_POS = new Pos(-37.5, 45, 54.5);
    private static final Pos PRESENT_INTERACTION_OFFSET = new Pos(0, -3, 0);

    private static final PlayerSkin NPC_SKIN = new PlayerSkin(
            "ewogICJ0aW1lc3RhbXAiIDogMTc2MzUzMDY4MTYyNywKICAicHJvZmlsZUlkIiA6ICI5ODA4NmFkMDNiZjc0YTA5OGY2YTEyMzkzOWJlZjhlNiIsCiAgInByb2ZpbGVOYW1lIiA6ICJwZXN0aWNpZGEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTY2NGU0ZmM5Zjc0MzQ4MWU3YTVlMDBiZjNkNDMwOGQ2NTQ0ZTJhOTgwZGVjZTI2NDFjM2VkODMxMWQ0MDAzZiIKICAgIH0KICB9Cn0=",
            "f4Gz5Dty+eaj1czUi4aZsbYPXqMDTtTqzeV6FN3j2ALF6XEcl9Xo3+Tt2k58ol/GtkbDuvCRR9OmMGyfPBfmJUq/8cfSa/j9AX9E/oVFgofRCDuaHjk46kFxdfsgGNevsxCc5Wkf9c+mad7VgkZ+12W2EjGQJjxgbeYHHpQZQ09oq1EwksN/zgnUdaOd3z+pfenBdTgfSHaqZHEQ/WRn7nKRbYkuuLY962Otud0r+OLFqIZ4peddEHN2/ZQWUCAgBvRP11LUCL+c9t3AZXnV3JK5vZOfPrTWimSE6trT8iBM6Lws6XzdJK2gwypGib49NicEjQq9ypI61/c8iMQYxUxFtFjLsdPhXuDg2Yb+z9Ufg6wllY4c3iILn2UP3oTpWocKD6GQszHva6da6Yw+UsUWc6b2P228USf7bJuj5vgzyVzpeJe7sTC3rj0RFbFGcxl0DKZJUeRFs8v7TRaIME90T/lCIlnVIsRAPqzg/hzHEJmx9InuGo1tz7p8uVOM1VazmF+lYrIEhZDd444Ey3yyVbbv8a+/1vLgMuNVB7nFyWwS5y2gxsw9beHJkJa2RXmkxmhQdJ/YhtmjIyZkqDbYGGUJXvOzgomNVu/YnBBcUD8zTa0j5L6niZEI1Zk5q34suYLuS5orEhhF5voj/PZTF07pW621rOgd2LxichI="
    );

    private float targetRotation = 0;

    @Override
    public void load(MapServer server, HubMapWorld world) {
        PresentConstants.init();

        var npc = new NpcPlayer("Advent Calendar", NPC_SKIN);
        npc.setNameTag(Component.text(BadSprite.require("icon/mouse_right").fontChar() + " View Advent Calendar"));
        npc.setEquipment(EquipmentSlot.HELMET, Hats.SANTA_HAT.impl().iconItem());
        npc.setInstance(world.instance(), NPC_POS);
        npc.setInteractionBox(3, 3);
        npc.setHandler((player, _, _, _) -> AdventCalanderPanel.open(player));

        var present = new NpcItemModel();
        present.setModel(PresentConstants.RED_GOLD_GREEN_TEXTURE);
        present.getEntityMeta().setScale(new Vec(5));
        present.setInstance(world.instance(), PRESENT_POS);
        present.setInteractionBox(5, 5, PRESENT_INTERACTION_OFFSET);
        present.setHandler((player, _, _, _) -> AdventCalanderPanel.open(player));

        server.scheduler().submitTask(() -> {
            var meta = present.getEntityMeta();
            meta.setNotifyAboutChanges(false);
            meta.setTransformationInterpolationStartDelta(0);
            meta.setTransformationInterpolationDuration(5 * ServerFlag.SERVER_TICKS_PER_SECOND);
            meta.setLeftRotation(new float[]{
                    0f,
                    (float) Math.sin(targetRotation / 2f),
                    0f,
                    (float) Math.cos(targetRotation / 2f),
            });
            meta.setNotifyAboutChanges(true);
            targetRotation += (float) Math.toRadians(90);

            return TaskSchedule.seconds(5);
        });

        if (server instanceof AbstractMapServer hub) {
            hub.commandManager().register(
                    SimpleCommand.of("advent")
                            .description("Open the advent calendar GUI")
                            .callback(AdventCalanderPanel::open)
                            .build()
            );
        }
    }

}
