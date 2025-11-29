package net.hollowcube.mapmaker.hub.feature.event.christmas;

import com.google.auto.service.AutoService;
import net.hollowcube.command.dsl.SimpleCommand;
import net.hollowcube.mapmaker.cosmetic.Hats;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.entity.NpcPlayer;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.gui.event.AdventCalendarPanel;
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
            "ewogICJ0aW1lc3RhbXAiIDogMTc2NDQwNjAxMDIwNCwKICAicHJvZmlsZUlkIiA6ICIyYmQyZTAxNGI5YWE0YzRjYmRmMzdmZjc5MzE2NDE5MSIsCiAgInByb2ZpbGVOYW1lIiA6ICJBdXRvU2tpbiIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS81Y2Y2MjM2YTE4MjExNzU4N2EwMDllNTk4MDBiMDJlN2MxMmI2ZTdkMzJkYWFiY2RmODY5YzIxNGU0YzQwZGIxIgogICAgfQogIH0KfQ==",
            "cm0ILLMjTaT58Jq8wxgxUkLwHSZLVKJYCXe/COo0EjBjj8i5GDZ7f7h+sQ71YKvV4jM00AYMfeBFUCqlJcoJ2DkJs7vTQ4S2+NoVmJWjRbLq6lJSJjq9eqs/n865kA8b4QkL/peL32/xh8Hn8Gop5P6ENebFj/ige+m6uhRWGIOL2s9xe76LFtimJC6KtuZ8Jydq3/+RYwi1wdfZBUxouFbSe8LvRbAR6Jy/jBaGJm/a8ckwZQUoq1vPKeS7FG3khWfq+pWoTXJLxxucsXrr3Fin0asU9zoSRUnIEannF7+zUT/VPRPwxxFMZ/v4M313a32jFDdBSwGCd+GPTZ++Jm6p2rKsl7SSECINnxd5/pLMUCTfp92/TZbirNpTNMEec9rDxe7QmtI0xh5WxqpJbal7COTejcyBif984fYX8DCTfhdo4vuSAe/WsrrP3zV0rbEmQ5x6ei8onIIll+hhszA0bS+0AygFbchHvc7LR5ukj2yDGMl+Ky276YrvaxbTTpce4fJcZtRaINFXA/ixIJwD0oZsVVjvBZEaIokNdGAEwzgzt6F05ZvquZac68Vb/Ae+CIYjAwMmoZA00c5tg1+cexbTjNntPn+EnlKX3UK89RHzvlWxwy4kns300KPYdwt/K1Jw/0Fu+RABPCj3XdC5SVjFZ8th9QSdU3maup4="
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
        npc.setHandler((player, _, _, _) -> AdventCalendarPanel.open(player));

        var present = new NpcItemModel();
        present.setModel(PresentConstants.RED_GOLD_GREEN_TEXTURE);
        present.getEntityMeta().setScale(new Vec(5));
        present.setInstance(world.instance(), PRESENT_POS);
        present.setInteractionBox(5, 5, PRESENT_INTERACTION_OFFSET);
        present.setHandler((player, _, _, _) -> AdventCalendarPanel.open(player));

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
                            .callback(AdventCalendarPanel::open)
                            .build()
            );
        }
    }

}
