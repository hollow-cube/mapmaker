package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.common.math.Quaternion;
import net.hollowcube.mapmaker.gui.store.StoreModule;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.BaseNpcEntity;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.scripting.ScriptEngine;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@AutoService(HubFeature.class)
public class StoreAdFeatureProvider implements HubFeature {
    private static final Pos STORE_AD_POS = new Pos(-4.5, 39, -29.5);

    private static final Pos GOLD_BLOCK_ENTITY_POS = new Pos(-4.5, 45, -29.5, 0, -90);
    private static final int GOLD_BLOCK_ENTITY_UPDATE_INTERVAL = 5; // Seconds

    private Supplier<ScriptEngine> scriptEngine;
    private PlayerService playerService;
    private PermManager permManager;

    private final NpcItemModel goldBlockEntity = new NpcItemModel();
    private int goldBlockEntityRotationTarget = 0;

    @Override
    public void load(@NotNull MapServer server, @NotNull HubMapWorld world) {
        this.scriptEngine = server::scriptEngine;
        this.playerService = server.playerService();
        this.permManager = server.permManager();

        var viewStoreEntity = BaseNpcEntity.createInteractionEntity(
                3, 4, this::handleStoreClick);
        viewStoreEntity.setInstance(world.instance(), STORE_AD_POS);

        goldBlockEntity.getEntityMeta().setItemStack(ItemStack.of(Material.GOLD_BLOCK)
                .with(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
        goldBlockEntity.getEntityMeta().setScale(new Vec(1));
        goldBlockEntity.setInstance(world.instance(), GOLD_BLOCK_ENTITY_POS);
        server.scheduler().submitTask(this::mapEntityUpdate, ExecutionType.TICK_START);
    }

    private void handleStoreClick(@NotNull Player player, @NotNull BaseNpcEntity npc, @NotNull PlayerHand hand, boolean isLeftClick) {
        if (hand != PlayerHand.MAIN) return;

        StoreModule.openStoreView(scriptEngine.get(), playerService, permManager, player, "hypercube");
    }

    private @NotNull TaskSchedule mapEntityUpdate() {
        var meta = goldBlockEntity.getEntityMeta();
        meta.setNotifyAboutChanges(false);

        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(20 * GOLD_BLOCK_ENTITY_UPDATE_INTERVAL);
        double verticalOffset = (goldBlockEntityRotationTarget % 360) / 90.0 * 0.25;
        if (verticalOffset > 1) verticalOffset = 2 - verticalOffset;
        meta.setTranslation(new Vec(0, 0, verticalOffset));
        meta.setRightRotation(new Quaternion(new Vec(0, 0, 1).normalize(), Math.toRadians(goldBlockEntityRotationTarget)).into());
        goldBlockEntityRotationTarget += 90;

        goldBlockEntity.sendPacketToViewers(new ParticlePacket(
                Particle.FIREWORK, goldBlockEntity.getPosition().add(0, verticalOffset, 0),
                new Vec(0.1), 0.1f, 5
        ));

        meta.setNotifyAboutChanges(true);
        return TaskSchedule.tick(GOLD_BLOCK_ENTITY_UPDATE_INTERVAL * 20);
    }

}
