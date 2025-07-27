package net.hollowcube.mapmaker.hub.feature.contest;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.cosmetic.Hats;
import net.hollowcube.mapmaker.gui.map.browser.MapContestBrowserView;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcPlayer;
import net.hollowcube.mapmaker.hub.entity.NpcTextModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.PlayerSkin;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@AutoService(HubFeature.class)
public class MapContest implements HubFeature {
    public static final int MAP_CONTEST_SLOT = -1;
    public static final String CONTEST_ID = "c9354e33-96c2-414a-9f4a-8c2ff4669086";

    public static final LocalDateTime BUTTON_UNLOCK_DATE = LocalDateTime.parse(
            "2025-07-13T17:00:00-04:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    public static final LocalDateTime START_DATE = LocalDateTime.parse(
            "2025-07-19T12:00:00-04:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    public static final LocalDateTime END_DATE = LocalDateTime.parse(
            "2025-08-09T12:00:00-04:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    private static final Pos TEXT_SPAWN_POS = new Pos(-37.5, 42 + 7, 54.5, 0, 0);
    private static final Pos NPC_SPAWN_POS = new Pos(-37.5, 40, 47.5, 180, 10);

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        scheduleAtStart(world, () -> {
            spawnText(world);
            spawnNpc(world);
        });
    }

    public static void scheduleAtStart(@NotNull HubMapWorld world, @NotNull Runnable task) {
        var millisToStart = ChronoUnit.MILLIS.between(LocalDateTime.now(), MapContest.START_DATE);
        if (millisToStart <= 0) {
            task.run();
            return;
        }
        world.instance().scheduler().buildTask(task)
             .delay(Duration.ofMillis(millisToStart))
             .schedule();
    }

    private void spawnText(@NotNull HubMapWorld world) {
        NpcTextModel text = new NpcTextModel();
        text.getEntityMeta().setText(Component.text("ɪᴛᴇᴍ ᴍᴀᴘ ᴄᴏᴍᴘᴇᴛɪᴛɪᴏɴ"));
        text.getEntityMeta().setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
        text.getEntityMeta().setScale(new Vec(5));
        text.setInstance(world.instance(), TEXT_SPAWN_POS);
    }

    private void spawnNpc(@NotNull HubMapWorld world) {
        var skin = new PlayerSkin(
                "ewogICJ0aW1lc3RhbXAiIDogMTc1MjkzMDYwNzY0MywKICAicHJvZmlsZUlkIiA6ICJkYmUyMTA1OTViMGI0MzEyYWExMjgzMGRjNTQwYWQwNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTZXRoMjgiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZGQwY2M1NWY3ZDlhOTZjZDZjMDJlNjA3Mjc2YzQ1OWJlZmNhMzM5NTJhMWQyMjJjN2U2NDlhNTRkYTY0ZTY4MyIKICAgIH0KICB9Cn0=",
                "ICiJEHabrK7GTdEQvmidVvzQgJ8dlD8yjgpPVuLYrDC/A4ku4UQPraX54NCTTsfAfJ5VW4v5+31OwF1nseJHOqxPoyWN5YxSStT5cdxAs6joHBovv6X5DOQ5IPeOtZ+gVqYUb0LevvMPlZK8YORYrspYGokPNZ0MWjbWQkpAInb1oPGI1VAl/1zPHvHXYeN9RQGgGCFFhyzdFg0q+YmNJtATc9iTfxfWn4trgtA7pNXCfDWl/EHguelR8jspZWBxNGx5qSlN5HqAEHfkSRix1Mn17eA4PkIljJ5LHyN68q+wF5N3RFPAf2Upzzh5rJ930ExGdXXKvmr/vk9Yqp4e9f+nqCi2GU+yTsJkTgy/uUMprc8/+RtAJRZK4hMk1C2wjdJgGmz9Q7Efj58tj840L9UXc6e3YHC2cs5RY0eu1oPRoTrtOXKW5O54EUKLZc+UhZFnhtSSiK2fOw/RUsWjLy26A3THenpq2fAU2CCuMgPCH2f+UnfF+IfUYMLqUm43qDFFp5lNDwA/pdXn6OyH5G/PB/CzDPRzCzNqDysEWxEd+07lOKe5Xi59ASX9JMRgTMEdoD/+b3Wd5vsNsLulygoji+FJwonqwHZlB0glLTYTl8TTpIMQQS6zFkpT1hlZysxF1VpHBHL1v3RR6k/8+63zpEhJ1qLiYZE1FG5sDas="
        );
        var playtimeNpc = new NpcPlayer("View Submissions", skin);
        playtimeNpc.setNameTag(Component.text(BadSprite.require("icon/mouse_right").fontChar() + " View Submissions"));
        playtimeNpc.setEquipment(EquipmentSlot.HELMET, Hats.HARD_HAT.impl().iconItem());
        playtimeNpc.setEquipment(EquipmentSlot.MAIN_HAND, ItemStack.of(Material.TRIDENT));
        playtimeNpc.setEquipment(EquipmentSlot.OFF_HAND, ItemStack.of(Material.WHITE_WOOL));
        playtimeNpc.setEquipment(EquipmentSlot.CHESTPLATE, ItemStack.of(Material.ELYTRA));
        playtimeNpc.setInstance(world.instance(), NPC_SPAWN_POS);
        playtimeNpc.setInteractionBox(3, 3);
        playtimeNpc.setHandler(MapContest::handleNpcClick);
    }

    private static void handleNpcClick(
            @NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull PlayerHand hand, boolean isLeftClick) {
        if (isLeftClick) return;
        openSubmissionMenu(player);
    }

    public static void openSubmissionMenu(@NotNull Player player) {
        var server = MapWorld.forPlayer(player).server();

        Panel.open(player, new MapContestBrowserView(
                server.playerService(), server.mapService(), server.bridge()
        ));
    }

}
