package net.hollowcube.map.event;

import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

public class EditWorldPlaceBlockEvent {

    public static void handleBlockPlacement(PlayerBlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData data = PlayerData.fromPlayer(player); //use method to retrieve player data
        int multibuild = data.getMultibuild();
        Instance instance = player.getInstance();
        Block block = event.getBlock();

        if (block.id() == Block.BARRIER.id()) {
            event.setCancelled(true);
            player.sendMessage("Barriers are not allowed!");
            return;
        }

        if (multibuild < 2) { // Check for minimum multibuild value
            return;
        }

        Vec playerFacing = event.getPlayer().getPosition().direction().normalize();

        // Convert to spherical coords (normalized so r = 1)
        double theta = Math.atan2(playerFacing.y(), playerFacing.x());
        double phi = Math.acos(playerFacing.z());

        double step90Radians = Math.toRadians(90);

        // Round the angles to nearest facing direction
        double roundedTheta = Math.round(theta / step90Radians) * step90Radians;
        double roundedPhi = Math.round(phi / step90Radians) * step90Radians;

        // Convert back to cartesian
        int x = (int) (Math.sin(roundedPhi) * Math.cos(roundedTheta));
        int y = (int) (Math.sin(roundedPhi) * Math.sin(roundedTheta));
        int z = (int) Math.cos(roundedPhi);

        Vec nearestFacing = new Vec(x, y, z);

        //player.sendMessage("facing_vec: " + nearestFacing);

        for (int i = 1; i < multibuild; i++) {
            Point targetPos = event.getBlockPosition().add(
                    nearestFacing.x() * i,
                    nearestFacing.y() * i,
                    nearestFacing.z() * i
            );
            instance.setBlock(targetPos, block);
            //TODO disallow or make compatibility for block facings and states, like stairs, slabs, buttons, etc.
        }
    }
}