package net.hollowcube.map.invites;

import net.hollowcube.map.MapServerBase;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.MapWorldManager;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInviteService {

    private record Invite(UUID inviterUUID, UUID inviteeUUID) {}
    private record Context(Instant time, String mapId) {}

    private static ConcurrentHashMap<Invite, Context> invites = new ConcurrentHashMap<>();
    private static final Duration expirationTime = Duration.of(5, ChronoUnit.MINUTES);
    private static MapWorldManager mwm;
    private static MapService ms;

    public static void init(MapWorldManager mapWorldManager, MapService mapService) {
        mwm = mapWorldManager;
        ms = mapService;
    }

    public static void registerInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var map = MapWorld.forPlayerOptional(inviter);
        if (map == null) {
            inviter.sendMessage("You must be in a map to invite a player!");
            return;
        }
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var val = new Context(now, map.map().id());
        if (context == null || Duration.between(context.time, now).compareTo(expirationTime) > 0)
            invites.put(key, val);
        inviter.sendMessage("Sent invite to " + PlayerDataV2.fromPlayer(invitee).displayName());
        invitee.sendMessage("You've been invited to " + map.map().name() + " by " + PlayerDataV2.fromPlayer(inviter).displayName());
    }

    public static void acceptInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var inviterMap = MapWorld.forPlayerOptional(inviter);
        var inviterName = PlayerDataV2.fromPlayer(inviter).displayName();
        if (context == null) {
            invitee.sendMessage("You don't have an invite to join " + inviterName + "!");
        } else if (inviterMap == null || !inviterMap.map().id().equals(context.mapId())) {
            invitee.sendMessage(inviterName + " has left the map!");
        } else if (Duration.between(context.time, now).compareTo(expirationTime) > 0) {
            invitee.sendMessage("That invite has expired!");
        } else {
            invites.remove(key);
            var map = ms.getMap(inviter.getUuid().toString(), context.mapId);
            if (map.isPublished()) {
                mwm.joinMap(invitee, map, HubToMapBridge.JoinMapState.PLAYING);
            } else {
                mwm.joinMap(invitee, map, HubToMapBridge.JoinMapState.EDITING);
            }
            inviter.sendMessage(PlayerDataV2.fromPlayer(invitee).displayName() + " has accepted your invite!");
        }
    }

    public static void rejectInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var inviterName = PlayerDataV2.fromPlayer(inviter).displayName();
        if (context == null || Duration.between(context.time, now).compareTo(expirationTime) > 0)
            invitee.sendMessage("No current invite from " + inviterName);
        else {
            invitee.sendMessage("Rejected invite from " + inviterName);
            inviter.sendMessage(PlayerDataV2.fromPlayer(invitee).displayName() + " has rejected your invite.");
        }
        invites.remove(key);
    }
}
