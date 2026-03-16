package net.hollowcube.mapmaker.hub.feature.misc;

import com.google.auto.service.AutoService;
import net.hollowcube.mapmaker.gui.map.browser.MapBrowserView;
import net.hollowcube.mapmaker.hub.HubMapWorld;
import net.hollowcube.mapmaker.hub.entity.NpcItemModel;
import net.hollowcube.mapmaker.hub.entity.util.InteractionEntity;
import net.hollowcube.mapmaker.hub.feature.HubFeature;
import net.hollowcube.mapmaker.hub.gui.edit.CreateMaps;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Metadata;
import net.minestom.server.entity.MetadataDef;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Map;
import java.util.function.Consumer;

@AutoService(HubFeature.class)
public class PlayMapStatueFeatureProvider implements HubFeature {

    private static final Pos LEFT = new Pos(-32.5, 38, 15.5);
    private static final Pos LEFT_MIDDLE = new Pos(-35.5, 38, 7.5);
    private static final Pos MIDDLE = new Pos(-36.5, 37, 0.5);
    private static final Pos RIGHT_MIDDLE = new Pos(-35.5, 38, -6.5);
    private static final Pos RIGHT = new Pos(-32.5, 38, -14.5);

    private static final double BASE_OFFSET = 1.8;
    private static final int ENTITY_UPDATE_INTERVAL = 5; // Seconds

    private @UnknownNullability MapServer server; // lateinit

    private final NpcItemModel[] edgeEntities = new NpcItemModel[5];
    private int entityHeightTarget = 0;

    @Override
    public void load(MapServer server, HubMapWorld world) {
        this.server = server;

        edgeEntities[0] = new NpcItemModel();
        edgeEntities[0].setModel(Material.DIAMOND, BadSprite.require("icon/map/create_map"));
        edgeEntities[0].setInstance(world.instance(), LEFT);
        appendInteractor(edgeEntities[0], 3, 5, this::handleCreateMapsClick, false);

        edgeEntities[3] = new NpcItemModel();
        edgeEntities[3].setModel(Material.DIAMOND, BadSprite.require("icon/map/best"));
        edgeEntities[3].setInstance(world.instance(), LEFT_MIDDLE);
        appendInteractor(edgeEntities[3], 3, 5, this::handleBestMapsClick, false);

        edgeEntities[4] = new NpcItemModel();
        edgeEntities[4].getEntityMeta().setItemStack(ItemStack.of(Material.STICK)
            .with(DataComponents.ITEM_MODEL, BadSprite.require("hub/5x5/blossom_itmg").model()));
        edgeEntities[4].getEntityMeta().setScale(new Vec(5)); // 5x because its 5x5x5
        edgeEntities[4].getEntityMeta().setTranslation(new Vec(0, 1 + BASE_OFFSET, 0));
        edgeEntities[4].setInstance(world.instance(), MIDDLE.withView(90, 0));
        appendInteractor(edgeEntities[4], 6, 6, this::handleQualityClick, true);

        edgeEntities[1] = new NpcItemModel();
        edgeEntities[1].setModel(Material.DIAMOND, BadSprite.require("icon/map/new"));
        edgeEntities[1].setInstance(world.instance(), RIGHT_MIDDLE);
        appendInteractor(edgeEntities[1], 3, 5, this::handleNewMapsClick, false);

        edgeEntities[2] = new NpcItemModel();
        edgeEntities[2].setModel(Material.DIAMOND, BadSprite.require("icon/map/search"));
        edgeEntities[2].setInstance(world.instance(), RIGHT);
        appendInteractor(edgeEntities[2], 3, 5, this::handleSearchMapsClick, false);

        // Configure the edge entities to be the same (not the center)
        for (int i = 0; i < 4; i++) {
            var meta = edgeEntities[i].getEntityMeta();
            meta.setScale(new Vec(3));
            meta.setTranslation(new Vec(0, BASE_OFFSET, 0));
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
        }

        server.scheduler().submitTask(this::entityUpdate, ExecutionType.TICK_START);
    }

    private void handleCreateMapsClick(Player player) {
        server.guiController().show(player, CreateMaps::new);
    }

    private void handleBestMapsClick(Player player) {
        var browser = new MapBrowserView(server.api(), server.mapService(), server.bridge(), false);
        Panel.open(player, browser);
        browser.simpleSort(MapBrowserView.SortPreset.BEST);
    }

    private void handleQualityClick(Player player) {
        var browser = new MapBrowserView(server.api(), server.mapService(), server.bridge(), false);
        Panel.open(player, browser);
        browser.simpleSort(MapBrowserView.SortPreset.QUALITY);
    }

    private void handleNewMapsClick(Player player) {
        var browser = new MapBrowserView(server.api(), server.mapService(), server.bridge(), false);
        Panel.open(player, browser);
        browser.simpleSort(MapBrowserView.SortPreset.NEW);
    }

    private void handleSearchMapsClick(Player player) {
        var browser = new MapBrowserView(server.api(), server.mapService(), server.bridge(), false);
        Panel.open(player, browser);
        // Set initial sort preset to best, else pagination doesn't initialize properly
        browser.simpleSort(MapBrowserView.SortPreset.BEST);
        browser.openSearchInput();
    }

    private void appendInteractor(Entity entity, int width, int height, Consumer<Player> onClick, boolean isCenter) {
        var entityId = entity.getEntityId();
        var interactionEntity = new InteractionEntity(width, height, 40, new InteractionEntity.Target() {
            @Override
            public void beginHover(Player player) {
                // Enable glowing - This works because we never set any other flags in this set, otherwise
                // it would be overridden when sending other metadata changes.
                player.sendPacket(new EntityMetaDataPacket(entityId, Map.of(
                    MetadataDef.ENTITY_FLAGS.index(),
                    Metadata.Byte((byte) 0x40)
                )));
            }

            @Override
            public void endHover(Player player) {
                // Disable glowing - See above for how/why this is functional.
                if (player.getPlayerConnection().getServerState() == ConnectionState.PLAY) {
                    player.sendPacket(new EntityMetaDataPacket(entityId, Map.of(
                        MetadataDef.ENTITY_FLAGS.index(),
                        Metadata.Byte((byte) 0x0)
                    )));
                }
            }

            @Override
            public void onRightClick(Player player) {
                onClick.accept(player);
            }
        });
        interactionEntity.setInstance(entity.getInstance(), entity.getPosition());
    }

    private TaskSchedule entityUpdate() {
        int updateInterval = (int) (20 * 0.5) * ENTITY_UPDATE_INTERVAL;

        {   // Spawn some particles around the center entity
        }

        for (int i = 0; i < edgeEntities.length; i++) {
            var meta = edgeEntities[i].getEntityMeta();
            meta.setNotifyAboutChanges(false);

            meta.setTransformationInterpolationStartDelta(0);
            meta.setTransformationInterpolationDuration(updateInterval);

            double verticalOffset = ((entityHeightTarget + i) % 8) * (1 / 8.0);
            if (verticalOffset > 0.5) verticalOffset = 1 - verticalOffset;
            var yTranslation = new Vec(0, (i == 4 ? 1 : 0) + BASE_OFFSET + (verticalOffset * 0.75), 0);
            meta.setTranslation(yTranslation);

            meta.setNotifyAboutChanges(true);

            // For the middle one we also want to spawn some extra particles
            // they look ugly as is
//            if (i == 4) {
//                edgeEntities[i].sendPacketToViewers(new ParticlePacket(
//                        Particle.DUST_COLOR_TRANSITION.withProperties(
//                                new Color(0xFFFFFF), new Color(0xFF0000), 1
//                        ), MIDDLE.add(0, yTranslation.y(), 0), new Vec(0.8),
//                        0f, 10
//                ));
//            }
        }

        entityHeightTarget += 1;
        return TaskSchedule.tick(updateInterval);
    }

}
