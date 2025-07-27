package net.hollowcube.mapmaker.runtime.building;

import net.hollowcube.mapmaker.map.PlayerState;
import net.hollowcube.mapmaker.runtime.item.MapDetailsItem;
import net.hollowcube.mapmaker.runtime.parkour.item.RateMapItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ReturnToHubItem;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Nullable;

public sealed interface BuildingState extends PlayerState<BuildingState, BuildingMapWorld> {

    record Building() implements BuildingState {
        
        @Override
        public void configurePlayer(BuildingMapWorld world, Player player, @Nullable BuildingState lastState) {
            BuildingState.super.configurePlayer(world, player, lastState);
            player.setAllowFlying(true);

            world.itemRegistry().setItemStack(player, MapDetailsItem.ID, 0);
            world.itemRegistry().setItemStack(player, RateMapItem.ID, 2);
            world.itemRegistry().setItemStack(player, ReturnToHubItem.ID, 8);
        }

    }
}
