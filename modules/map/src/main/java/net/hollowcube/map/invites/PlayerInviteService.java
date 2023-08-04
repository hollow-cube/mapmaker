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
        var acepteeName = PlayerDataV2.fromPlayer(acceptee).displayName();

        if (requests.get(requestKey) == null && invites.get(inviteKey) == null) {
            accepter.sendMessage(Component.translatable("map.invite_and_request.cant_send", acepteeName));
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
            rejecter.sendMessage(Component.translatable("map.invite_and_request.cant_reject"));
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
        var inviterName = PlayerDataV2.fromPlayer(inviter).displayName();
        var inviteeName = PlayerDataV2.fromPlayer(invitee).displayName();
        if (inviterMap == null) {
            inviter.sendMessage(Component.translatable("map.invite.no_map"));
            return;
        } else if (inviteeMap == inviterMap) {
            inviter.sendMessage(Component.translatable("map.invite.same_map", inviteeName));
            return;
        }
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var val = new Context(now, inviterMap.map().id());

        if (context == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0)
            invites.put(key, val);
        if (!inviterMap.map().isPublished()) {
            inviter.sendMessage(Component.translatable("map.build.invite.sent", inviteeName, Component.text(inviterMap.map().name())));
            invitee.sendMessage(Component.translatable("map.build.invite.pending", inviterName, Component.text(inviterMap.map().name())));
        } else {
            inviter.sendMessage(Component.translatable("map.play.invite.sent", inviteeName, Component.text(inviterMap.map().name())));
            invitee.sendMessage(Component.translatable("map.play.invite.pending", inviterName, Component.text(inviterMap.map().name())));
        }
    }

    private static void acceptInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var inviterMap = MapWorld.forPlayerOptional(inviter);
        var inviterName = PlayerDataV2.fromPlayer(inviter).displayName();
        var inviteeName = PlayerDataV2.fromPlayer(invitee).displayName();
        if (context == null) {
            invitee.sendMessage(Component.translatable("map.invite.no_join", inviterName));
        } else if (inviterMap == null || !inviterMap.map().id().equals(context.mapId())) {
            invitee.sendMessage(Component.translatable("map.invite.left_map", inviterName));
        } else if (Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0) {
            invitee.sendMessage("map.invite.expired");
        } else {
            invites.remove(key);
            var map = ms.getMap(inviter.getUuid().toString(), context.mapId);
            if (map.isPublished()) {
                mwm.joinMap(invitee, map, false, false);
                inviter.sendMessage(Component.translatable("map.play.invite.accepted", inviteeName, Component.text(inviterMap.map().name())));
                invitee.sendMessage(Component.translatable("map.play.invite.accept", inviterName, Component.text(inviterMap.map().name())));
            } else {
                mwm.joinMap(invitee, map, true, false);
                inviter.sendMessage(Component.translatable("map.build.invite.accepted", inviteeName, Component.text(inviterMap.map().name())));
                invitee.sendMessage(Component.translatable("map.build.invite.accept", inviterName, Component.text(inviterMap.map().name())));
            }
        }
    }

    public static void registerRequest(@NotNull Player requester, @NotNull Player requestee) {
        var requesteeMap = MapWorld.forPlayerOptional(requestee);
        var requesterMap = MapWorld.forPlayerOptional(requester);
        var requesteeName = PlayerDataV2.fromPlayer(requestee).displayName();
        var requesterName = PlayerDataV2.fromPlayer(requester).displayName();
        if (requesteeMap == null) {
            requester.sendMessage(Component.translatable("map.play.request.cant_send", PlayerDataV2.fromPlayer(requestee).displayName()));
            return;
        } else if (requesteeMap == requesterMap) {
            requester.sendMessage(Component.translatable("map.request.same_map", requesteeName));
            return;
        }
        var key = new Request(requester.getUuid(), requestee.getUuid());
        var context = requests.get(key);
        var now = Instant.now();
        var val = new Context(now, requesteeMap.map().id());
        if (context == null || Duration.between(context.time, now).compareTo(requestExpirationTime) > 0)
            requests.put(key, val);
        if (!requesteeMap.map().isPublished()) {
            requester.sendMessage(Component.translatable("map.build.request.sent", requesteeName, Component.text(requesteeMap.map().name())));
            requestee.sendMessage(Component.translatable("map.build.request.pending", requesterName, Component.text(requesteeMap.map().name())));
        } else {
            requester.sendMessage(Component.translatable("map.play.request.sent", requesteeName, Component.text(requesteeMap.map().name())));
            requestee.sendMessage(Component.translatable("map.play.request.pending", requesterName, Component.text(requesteeMap.map().name())));
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
            requestee.sendMessage(Component.translatable("map.request.no_join", requesterName));
        } else if (requesteeMap == null || !requesteeMap.map().id().equals(context.mapId())) {
            requester.sendMessage(Component.translatable("map.invite.left_map", requesteeName));
        } else if (Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0) {
            requestee.sendMessage(Component.translatable("map.invite.expired"));
        } else {
            requests.remove(key);
            var map = ms.getMap(requester.getUuid().toString(), context.mapId);
            if (map.isPublished()) {
                mwm.joinMap(requester, map, false, false);
                requester.sendMessage(Component.translatable("map.play.request.accepted", requesteeName, Component.text(requesteeMap.map().name())));
                requestee.sendMessage(Component.translatable("map.play.request.accept", requesterName, Component.text(requesteeMap.map().name())));
            } else {
                mwm.joinMap(requester, map, true, false);
                requester.sendMessage(Component.translatable("map.build.request.accepted", requesteeName, Component.text(requesteeMap.map().name())));
                requestee.sendMessage(Component.translatable("map.build.request.accept", requesterName, Component.text(requesteeMap.map().name())));
            }
        }
    }

    private static void rejectInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var key = new Invite(inviter.getUuid(), invitee.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var inviteeMap = MapWorld.forPlayerOptional(invitee);
        var inviterName = PlayerDataV2.fromPlayer(inviter).displayName();
        var inviteeName = PlayerDataV2.fromPlayer(invitee).displayName();
        if (context == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0) {
            invitee.sendMessage(Component.translatable("map.request.no_join", inviterName));
        } else {
            invitee.sendMessage(Component.translatable("map.play.invite.deny", inviterName, Component.text(inviteeMap.map().name())));
            inviter.sendMessage(Component.translatable("map.play.invite.denied", inviteeName, Component.text(inviteeMap.map().name())));
        }
        invites.remove(key);
    }

    private static void rejectRequest(@NotNull Player requester, @NotNull Player requestee) {
        var key = new Request(requester.getUuid(), requestee.getUuid());
        var context = requests.get(key);
        var now = Instant.now();
        var requesteeMap = MapWorld.forPlayerOptional(requestee);
        var inviterName = PlayerDataV2.fromPlayer(requester).displayName();
        if (context == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0)
            requestee.sendMessage(Component.translatable("map.request.no_join", inviterName));
        else {
            requestee.sendMessage(Component.translatable("map.play.request.deny", inviterName, Component.text(requesteeMap.map().name())));
            requester.sendMessage(Component.translatable("map.play.request.denied", PlayerDataV2.fromPlayer(requestee).displayName(), Component.text(requesteeMap.map().name())));
        }
        requests.remove(key);
    }

    public static void invalidateInvitesAndRequests(Player invalidater) {
        invites.forEach((invite, context) -> {
            if (invite.inviterUUID().equals(invalidater.getUuid()) || invite.inviteeUUID().equals(invalidater.getUuid())) {
                invites.remove(invite);
                // TODO messages
            }
        });

        requests.forEach((request, context) -> {
            if (request.requesterUUID().equals(invalidater.getUuid()) || request.requesteeUUID().equals(invalidater.getUuid())) {
                requests.remove(request);
                // TODO messages
            }
        });
    }
}
