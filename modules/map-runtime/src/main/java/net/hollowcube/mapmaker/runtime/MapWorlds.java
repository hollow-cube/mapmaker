package net.hollowcube.mapmaker.runtime;

import net.hollowcube.ipc.map.MapData;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.MapServer;
import net.hollowcube.mapmaker.map.block.vanilla.DripleafBlock;
import net.hollowcube.mapmaker.map.entity.potion.PotionHandler;
import net.hollowcube.mapmaker.runtime.building.BuildingMapWorld;
import net.hollowcube.mapmaker.runtime.freeform.FreeformMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.HotbarItems;
import net.hollowcube.mapmaker.runtime.parkour.block.ClientBlockPlacementListener;
import net.hollowcube.mapmaker.runtime.parkour.block.PressurePlateBlock;
import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.network.packet.client.play.ClientPlayerBlockPlacementPacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Nullable;

/// Creates the world a map is played in, and sets up what every played world needs from the server process.
public final class MapWorlds {
    private static @Nullable ServerProcess initProcess = null;

    public static AbstractMapWorld<?, ?> forPlaying(MapServer server, MapData map) {
        return switch (map.settings().variant()) {
            case PARKOUR -> new ParkourMapWorld(server, map);
            case BUILDING -> new BuildingMapWorld(server, map);
            case FREEFORM -> new FreeformMapWorld(server, map);
            default -> throw new IllegalStateException(
                "No world for map variant " + map.settings().variant()
            );
        };
    }

    public static void initGlobalReferences() {
        var process = MinecraftServer.process();
        if (initProcess == process) return;
        initProcess = process;

        // Force init. Very gross should fix
        var _ = Attachments.PROGRESS_INDEX;
        var _ = HotbarItems.CODEC;
        var _ = ActionTriggerData.CODEC;

        process.packetListener().setPlayListener(
            ClientPlayerBlockPlacementPacket.class,
            ClientBlockPlacementListener::handleBlockPlacementPacket
        );

        process.block().registerHandler(
            PressurePlateBlock.INSTANCE.getKey(),
            () -> PressurePlateBlock.INSTANCE
        );

        process.block().registerHandler(
            DripleafBlock.INSTANCE.getKey(),
            () -> DripleafBlock.INSTANCE
        );

        MinecraftServer.getGlobalEventHandler().addChild(PotionHandler.EVENT_NODE);
    }

    public static void checkInitialized() {
        Check.stateCondition(
            initProcess == null,
            "Played worlds are not initialized, was `MapWorlds.initGlobalReferences()` called?"
        );
    }

    private MapWorlds() {}
}
