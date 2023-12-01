package net.hollowcube.map.invites;

import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.MapWorldManager;
import net.hollowcube.mapmaker.bridge.HubToMapBridge;
import net.hollowcube.mapmaker.invite.PlayerInviteService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.apache.kafka.common.metrics.stats.Min;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInviteServiceImpl implements PlayerInviteService {

    private record Invite(UUID inviterUUID, UUID inviteeUUID) {}

    private record Request(UUID requesterUUID, UUID requesteeUUID) {}

    private record Context(Instant time, String mapId) {}

    private final MapWorldManager mwm;
    private final ConcurrentHashMap<Invite, Context> invites = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Request, Context> requests = new ConcurrentHashMap<>();
    private final Duration inviteExpirationTime = Duration.of(5, ChronoUnit.MINUTES);
    private final Duration requestExpirationTime = Duration.of(1, ChronoUnit.MINUTES);

    public PlayerInviteServiceImpl(MapWorldManager mwm) {
        this.mwm = mwm;
    }

    @Override
    public void join(@NotNull Player sender, @NotNull Player target) {
        var targetMap = MapWorld.forPlayerOptional(target);
        if (targetMap == null) {
            sender.sendMessage(Component.translatable("command.join.only_playing"));
        } else {
            mwm.joinMap(sender, targetMap.map(), HubToMapBridge.JoinMapState.PLAYING);
        }
    }

    @Override
    public void accept(@NotNull Player sender) {
        // If we only have the sender, search for 1 appropriate match (uuid).
        // Fail if we have more than one
        // Find valid matches
        List<Invite> inviteList = new ArrayList<>();
        List<Request> requestList = new ArrayList<>();
        Request currentRequest = null;
        for (Invite invite : invites.keySet()) {
            if (invite.inviteeUUID.equals(sender.getUuid())) {
                inviteList.add(invite);
                if (inviteList.size() > 1) {
                    if (!inviteList.get(0).inviterUUID.equals(invite.inviterUUID)) {
                        sender.sendMessage("You have multiple invites, please specify which player to accept from."); // TODO Translate
                        return;
                    }
                }
            }
        }
        for (Request request : requests.keySet()) {
            if (request.requesterUUID.equals(sender.getUuid())) {
                requestList.add(request);
                if (requestList.size() > 1) {
                    if (!requestList.get(0).requesteeUUID.equals(request.requesteeUUID)) {
                        sender.sendMessage("You have multiple requests, please specify which player to accept from."); // TODO Translate
                        return;
                    }
                }
            }
        }
        // We know that the invite list and request list have the same inviterUUIDs/requesteeUUID, so we need to compare the lists (if necessary)
        if (!inviteList.isEmpty() && !requestList.isEmpty()) {
            // Compare
            if (!inviteList.get(0).inviterUUID.equals(requestList.get(0).requesteeUUID)) {
                sender.sendMessage("You have an distinct request and invite, please specify which player to accept from."); // TODO Translate
                return;
            }
            Player target = MinecraftServer.getConnectionManager().getPlayer(inviteList.get(0).inviterUUID);
            if (target == null) {
                sender.sendMessage("Invalid invite, inviter is offline."); // TODO Translate
                return;
            }
            acceptInvite(sender, target);
            for (Request request : requestList) {
                requests.remove(request);
            }
        } else if (!inviteList.isEmpty()) {
            Player target = MinecraftServer.getConnectionManager().getPlayer(inviteList.get(0).inviterUUID);
            if (target == null) {
                sender.sendMessage("Invalid invite, inviter is offline."); // TODO Translate
                return;
            }
            acceptInvite(sender, target);
        } else if (!requestList.isEmpty()) {
            Player target = MinecraftServer.getConnectionManager().getPlayer(requestList.get(0).requesteeUUID);
            if (target == null) {
                sender.sendMessage("Invalid request, inviter is offline."); // TODO Translate
                return;
            }
            acceptRequest(sender, target);
        } else {
            // Nothing to accept
            sender.sendMessage(Component.translatable("map.invite_and_request.cant_accept"));
        }
    }

    @Override
    public void accept(@NotNull Player sender, @NotNull Player target) {
        // Sender is the one accepting it
        var inviteKey = new Invite(target.getUuid(), sender.getUuid());
        var requestKey = new Request(target.getUuid(), sender.getUuid());

        if (invites.get(inviteKey) != null && requests.get(requestKey) != null) {
            // Whichever came later wins
            var inviteTime = invites.get(inviteKey).time;
            var requestTime = requests.get(requestKey).time;
            if (inviteTime.compareTo(requestTime) > 0) {
                acceptInvite(target, sender);
                requests.remove(requestKey);
            } else {
                acceptRequest(target, sender);
                invites.remove(inviteKey);
            }
        } else if (requests.get(requestKey) != null) {
            acceptRequest(target, sender);
        } else if (invites.get(inviteKey) != null) {
            acceptInvite(target, sender);
        } else {
            sender.sendMessage(Component.translatable("map.invite_and_request.cant_accept"));
        }
    }

    @Override
    public void reject(@NotNull Player sender, @NotNull Player target) {
        var inviteKey = new Invite(target.getUuid(), sender.getUuid());
        var requestKey = new Request(target.getUuid(), sender.getUuid());

        if (invites.get(inviteKey) == null && requests.get(requestKey) == null) {
            sender.sendMessage(Component.translatable("map.invite_and_request.cant_reject"));
            return;
        }
        if (invites.get(inviteKey) != null) {
            rejectInvite(target, sender);
        }
        if (requests.get(requestKey) != null) {
            rejectRequest(target, sender);
        }
    }

    @Override
    public void registerInvite(@NotNull Player sender, @NotNull Player target) {
        var senderMap = MapWorld.forPlayerOptional(sender);
        if (senderMap == null) {
            sender.sendMessage(Component.translatable("map.invite.no_map"));
            return;
        }
        var targetDisplayName = PlayerDataV2.fromPlayer(target).displayName();
        if (!doesPlayerOwnMap(sender, senderMap) && !senderMap.map().isPublished()) {
            sender.sendMessage(Component.translatable("map.invite.no_permission", targetDisplayName, Component.text(senderMap.map().name())));
            return;
        }

        var targetMap = MapWorld.forPlayerOptional(target);
        var senderDisplayName = PlayerDataV2.fromPlayer(sender).displayName();
        var senderName = Component.text(PlayerDataV2.fromPlayer(sender).username());
        if (targetMap == senderMap) {
            sender.sendMessage(Component.translatable("map.invite.same_map"));
            return;
        }

        var key = new Invite(sender.getUuid(), target.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var val = new Context(now, senderMap.map().id());

        if (context != null && Duration.between(context.time, now).compareTo(inviteExpirationTime) < 0) {
            // if we already have a valid invite
            sender.sendMessage(Component.translatable("map.invite.already_present", targetDisplayName));
            return;
        } else {
            invites.put(key, val);
        }

        // build/play determined by map publish state
        String translateString = "map." + (senderMap.map().isPublished() ? "play" : "build") + ".invite.";
        sender.sendMessage(Component.translatable(translateString + "sent", targetDisplayName, Component.text(senderMap.map().name())));
        target.sendMessage(Component.translatable(translateString + "pending", senderDisplayName, Component.text(senderMap.map().name()), senderName));
    }

    private void acceptInvite(@NotNull Player sender, @NotNull Player target) {
        var key = new Invite(sender.getUuid(), target.getUuid());
        var context = invites.get(key);
        var senderDisplayName = PlayerDataV2.fromPlayer(sender).displayName();
        if (context == null) {
            target.sendMessage(Component.translatable("map.invite.no_join", senderDisplayName));
            return;
        }

        var now = Instant.now();
        var senderMap = MapWorld.forPlayerOptional(sender);
        var targetDisplayName = PlayerDataV2.fromPlayer(target).displayName();
        if (senderMap == null || !senderMap.map().id().equals(context.mapId())) {
            target.sendMessage(Component.translatable("map.invite.left_map", senderDisplayName));
        } else if (Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0) {
            target.sendMessage("map.invite.expired");
        } else {
            // build/play determined by map publish state
            String translateString = "map." + (senderMap.map().isPublished() ? "play" : "build") + ".invite.";
            sender.sendMessage(Component.translatable(translateString + "accepted", targetDisplayName, Component.text(senderMap.map().name())));
            target.sendMessage(Component.translatable(translateString + "accept", senderDisplayName, Component.text(senderMap.map().name())));
            // We can use senderMap.map instead of retrieving from the map service again because we know the ids are equal from the above clause
            mwm.joinMap(target, senderMap.map(), (senderMap.map().isPublished() ? HubToMapBridge.JoinMapState.PLAYING : HubToMapBridge.JoinMapState.EDITING));
        }
        // Remove if they left the map, it expired, or it was successful
        invites.remove(key);
    }

    @Override
    public void registerRequest(@NotNull Player sender, @NotNull Player target) {
        var senderMap = MapWorld.forPlayerOptional(sender);
        var targetMap = MapWorld.forPlayerOptional(target);
        var targetDisplayName = PlayerDataV2.fromPlayer(target).displayName();
        var senderDisplayName = PlayerDataV2.fromPlayer(sender).displayName();
        var senderName = Component.text(PlayerDataV2.fromPlayer(sender).username());
        if (targetMap == null) {
            sender.sendMessage(Component.translatable("map.play.request.cant_send", targetDisplayName));
            return;
        } else if (senderMap == targetMap) {
            sender.sendMessage(Component.translatable("map.request.same_map", targetDisplayName));
            return;
        }
        var key = new Request(sender.getUuid(), target.getUuid());
        var context = requests.get(key);
        var now = Instant.now();
        var val = new Context(now, targetMap.map().id());

        if (context != null && Duration.between(context.time, now).compareTo(requestExpirationTime) < 0) {
            // if we already have a valid request
            sender.sendMessage(Component.translatable("map.request.already_present", targetDisplayName));
            return;
        } else {
            requests.put(key, val);
        }

        // build/play determined by map publish state
        String translateString = "map." + (targetMap.map().isPublished() ? "play" : "build") + ".request.";
        sender.sendMessage(Component.translatable(translateString + "sent", targetDisplayName, Component.text(targetMap.map().name())));
        target.sendMessage(Component.translatable(translateString + "pending", senderDisplayName, Component.text(targetMap.map().name()), senderName));
    }

    private void acceptRequest(@NotNull Player sender, @NotNull Player target) {
        // Sender and target are swapped around by accept()
        var key = new Request(sender.getUuid(), target.getUuid());
        var context = requests.get(key);
        if (context != null) {
            System.out.println("Found context");
        }
        var senderDisplayName = PlayerDataV2.fromPlayer(sender).displayName();
        if (context == null) {
            target.sendMessage(Component.translatable("map.request.no_join", senderDisplayName));
            return;
        }
        var now = Instant.now();
        var targetMap = MapWorld.forPlayerOptional(target);

        if (targetMap == null || !targetMap.map().id().equals(context.mapId())) {
            target.sendMessage(Component.translatable("map.invite.left_map", senderDisplayName));
        } else if (Duration.between(context.time, now).compareTo(requestExpirationTime) > 0) {
            target.sendMessage(Component.translatable("map.request.expired"));
        } else {
            var targetDisplayName = PlayerDataV2.fromPlayer(target).displayName();
            // build/play determined by map publish state
            String translateString = "map." + (targetMap.map().isPublished() ? "play" : "build") + ".request.";
            sender.sendMessage(Component.translatable(translateString + "accepted", targetDisplayName, Component.text(targetMap.map().name())));
            target.sendMessage(Component.translatable(translateString + "accept", senderDisplayName, Component.text(targetMap.map().name())));
            // We can use senderMap.map instead of retrieving from the map service again because we know the ids are equal from the above clause
            mwm.joinMap(sender, targetMap.map(), (targetMap.map().isPublished() ? HubToMapBridge.JoinMapState.PLAYING : HubToMapBridge.JoinMapState.EDITING));
        }
        requests.remove(key);
    }

    private void rejectInvite(@NotNull Player sender, @NotNull Player target) {
        var key = new Invite(sender.getUuid(), target.getUuid());
        var context = invites.get(key);
        var now = Instant.now();
        var senderMap = MapWorld.forPlayerOptional(sender);
        var senderDisplayName = PlayerDataV2.fromPlayer(sender).displayName();
        if (context == null || senderMap == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0) {
            target.sendMessage(Component.translatable("map.request.no_join", senderDisplayName));
        } else {
            target.sendMessage(Component.translatable("map.play.invite.deny", senderDisplayName, Component.text(senderMap.map().name())));
            sender.sendMessage(Component.translatable("map.play.invite.denied", PlayerDataV2.fromPlayer(target).displayName(), Component.text(senderMap.map().name())));
            invites.remove(key);
        }
    }

    private void rejectRequest(@NotNull Player sender, @NotNull Player target) {
        var key = new Request(sender.getUuid(), target.getUuid());
        var context = requests.get(key);
        var now = Instant.now();
        var targetMap = MapWorld.forPlayerOptional(target);
        var senderDisplayName = PlayerDataV2.fromPlayer(sender).displayName();
        if (context == null || targetMap == null || Duration.between(context.time, now).compareTo(inviteExpirationTime) > 0)
            target.sendMessage(Component.translatable("map.request.no_join", senderDisplayName));
        else {
            target.sendMessage(Component.translatable("map.play.request.deny", senderDisplayName, Component.text(targetMap.map().name())));
            sender.sendMessage(Component.translatable("map.play.request.denied", PlayerDataV2.fromPlayer(target).displayName(), Component.text(targetMap.map().name())));
            requests.remove(key);
        }
    }

    private boolean doesPlayerOwnMap(@NotNull Player player, @NotNull MapWorld mapWorld) {
        return player.getUuid().equals(UUID.fromString(mapWorld.map().owner()));
    }
}
