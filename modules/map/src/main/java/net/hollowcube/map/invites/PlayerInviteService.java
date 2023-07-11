package net.hollowcube.map.invites;

import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.MapWorldManager;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInviteService {

    private record Invite(UUID inviterUUID, UUID inviteeUUID) {}
    private record Request(UUID requesterUUID, UUID requesteeUUID) {}
    private record Context(Instant time, String mapId) {}

    private static ConcurrentHashMap<Invite, Context> invites = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Request, Context> requests = new ConcurrentHashMap<>();
    private static final Duration inviteExpirationTime = Duration.of(5, ChronoUnit.MINUTES);
    private static final Duration requestExpirationTime = Duration.of(1, ChronoUnit.MINUTES);
    private static MapWorldManager mwm;
    private static MapService ms;

    public static void init(MapWorldManager mapWorldManager, MapService mapService) {
        mwm = mapWorldManager;
        ms = mapService;
    }

    public static void accept(@NotNull Player accepter, @NotNull Player acceptee) {
        var inviteKey = new Invite(acceptee.getUuid(), accepter.getUuid());
        var requestKey = new Request(acceptee.getUuid(), accepter.getUuid());

        if (requests.get(requestKey) == null && invites.get(inviteKey) == null) {
            accepter.sendMessage("You do not have any requests or invites!"); //todo all translation keys
        } else if (invites.get(inviteKey) == null) {
            acceptRequest(acceptee, accepter);
        } else if (requests.get(requestKey) == null) {
            acceptInvite(acceptee, accepter);
        } else {
            var inviteTime = invites.get(inviteKey).time;
            var requestTime = requests.get(requestKey).time;
            if (inviteTime.compareTo(requestTime) > 0) {
                acceptInvite(acceptee, accepter);
                requests.remove(requestKey);
            } else {
                acceptRequest(acceptee, accepter);
                invites.remove(inviteKey);
            }
        }
    }

    public static void reject(@NotNull Player rejecter, @NotNull Player rejectee) {
        var inviteKey = new Invite(rejectee.getUuid(), rejecter.getUuid());
        var requestKey = new Request(rejectee.getUuid(), rejecter.getUuid());

        if (invites.get(inviteKey) == null && requests.get(requestKey) == null) {
            rejecter.sendMessage("You do not have any requests or invites to reject!");
            return;
        }
        if (invites.get(inviteKey) != null) {
            rejectInvite(rejectee, rejecter);
        }
        if (requests.get(requestKey) != null) {
            rejectRequest(rejectee, rejecter);
        }
    }

    public static void registerInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var inviterMap = MapWorld.forPlayerOptional(inviter);
        var inviteeMap = MapWorld.forPlayerOptional(invitee);
        if (inviterMap == null) {
            inviter.sendMessage("You must be in a map to invite a player!");
            return;
        } else if (inviteeMap == inviterMap) {
            inviter.sendMessage("This player is already in your map!");
            return;
        }
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var val = new Context(now, inviterMap.map().id());

        if (context == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0)
            invites.put(key, val);
        inviter.sendMessage("Sent invite to " + PlayerDataV2.fromPlayer(invitee).displayName() + ".");
        if (!inviterMap.map().isPublished()) {
            invitee.sendMessage(Component.translatable("create_maps.invite.sent", PlayerDataV2.fromPlayer(inviter).displayName(), Component.text(inviterMap.map().name())));
        } else {
            invitee.sendMessage("You've been invited to play " + inviterMap.map().name() + " by " + PlayerDataV2.fromPlayer(inviter).displayName() + ".");
        }
    }

    private static void acceptInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var inviterMap = MapWorld.forPlayerOptional(inviter);
        var inviterName = PlayerDataV2.fromPlayer(inviter).displayName();
        if (context == null) {
            invitee.sendMessage("You don't have an invite to join " + inviterName + ".");
        } else if (inviterMap == null || !inviterMap.map().id().equals(context.mapId())) {
            invitee.sendMessage(inviterName + " has left the map!");
        } else if (Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0) {
            invitee.sendMessage("That invite has expired!");
        } else {
            invites.remove(key);
            var map = ms.getMap(inviter.getUuid().toString(), context.mapId);
            if (map.isPublished()) {
                mwm.joinMap(invitee, map, false, false);
            } else {
                mwm.joinMap(invitee, map, true, false);
            }
            inviter.sendMessage(PlayerDataV2.fromPlayer(invitee).displayName() + " has accepted your invite!");
        }
    }

    public static void registerRequest(@NotNull Player requester, @NotNull Player requestee) {
        var requesteeMap = MapWorld.forPlayerOptional(requestee);
        var requesterMap = MapWorld.forPlayerOptional(requester);
        if (requesteeMap == null) {
            requester.sendMessage(PlayerDataV2.fromPlayer(requestee).displayName() + " must be in a map for you to request to join them!");
            return;
        } else if (requesteeMap == requesterMap) {
            requester.sendMessage("You are already in the same map as that player!");
            return;
        }
        var key = new Request(requester.getUuid(), requestee.getUuid());
        var context = requests.get(key);
        var now = Instant.now();
        var val = new Context(now, requesteeMap.map().id());
        if (context == null || Duration.between(context.time, now).compareTo(requestExpirationTime) > 0)
            requests.put(key, val);
        requester.sendMessage("Sent request to join " + PlayerDataV2.fromPlayer(requestee).displayName());
        if (!requesteeMap.map().isPublished()) {
            requestee.sendMessage(PlayerDataV2.fromPlayer(requester).displayName() + " wants to build with you on  " + requesteeMap.map().name() + ".");
        } else {
            requestee.sendMessage(PlayerDataV2.fromPlayer(requester).displayName() + " wants to play with you on " + requesteeMap.map().name() + ".");
        }
    }

    private static void acceptRequest(@NotNull Player requester, @NotNull Player requestee) {
        var key = new Request(requester.getUuid(), requestee.getUuid());
        var context = requests.get(key);
        var now = Instant.now();
        var requesteeMap = MapWorld.forPlayerOptional(requestee);
        var requesteeName = PlayerDataV2.fromPlayer(requestee).displayName();
        var requesterName = PlayerDataV2.fromPlayer(requester).displayName();
        if (context == null) {
            requestee.sendMessage("You don't have a request to join " + requesterName + "!");
            System.out.println("2");
        } else if (requesteeMap == null || !requesteeMap.map().id().equals(context.mapId())) {
            requester.sendMessage(requesteeName + " has left the map!");
            System.out.println("2.5");
        } else if (Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0) {
            requestee.sendMessage(Component.translatable("create_maps.invite.expired"));
            System.out.println("2.75");
        } else {
            requests.remove(key);
            var map = ms.getMap(requester.getUuid().toString(), context.mapId);
            if (map.isPublished()) {
                mwm.joinMap(requester, map, false, false);
                System.out.println("2.8");
            } else {
                mwm.joinMap(requester, map, true, false);
                System.out.println("2.9");
            }
            requester.sendMessage(PlayerDataV2.fromPlayer(requestee).displayName() + " has accepted your request!");
            System.out.println("2.99");
        }
    }

    private static void rejectInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var inviterName = PlayerDataV2.fromPlayer(inviter).displayName();
        if (context == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0)
            invitee.sendMessage("No current invite from " + inviterName + ".");
        else {
            invitee.sendMessage("Rejected invite from " + inviterName + ".");
            inviter.sendMessage(PlayerDataV2.fromPlayer(invitee).displayName() + " has rejected your invite.");
        }
        invites.remove(key);
    }

    private static void rejectRequest(@NotNull Player requester, @NotNull Player requestee) {
        var key = new Request(requester.getUuid(), requestee.getUuid());
        var context = requests.get(key);
        var now = Instant.now();
        var inviterName = PlayerDataV2.fromPlayer(requester).displayName();
        if (context == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0)
            requestee.sendMessage("No current request from " + inviterName + ".");
        else {
            requestee.sendMessage("Rejected request from " + inviterName + ".");
            requester.sendMessage(PlayerDataV2.fromPlayer(requestee).displayName() + " has rejected your request to join them.");
        }
        requests.remove(key);
    }
}
