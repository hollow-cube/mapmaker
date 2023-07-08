package net.hollowcube.map.invites;

import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInviteService {
    enum Response {
        OUTSTANDING,
        REJECTED,
        ACCEPTED,
    }

    private static ConcurrentHashMap<Map<Player, Player>, Response> invites;

    public PlayerInviteService() {
        MinecraftServer.getSchedulerManager()
                .buildTask(PlayerInviteService::tick)
                .executionType(ExecutionType.ASYNC)
                .repeat(TaskSchedule.seconds(1))
                .schedule();
    }

    public static void registerInvite(@NotNull Player inviter, @NotNull Player invitee) {
        var inviteStatus = invites.get(Map.of(inviter, invitee));
        if (inviteStatus != Response.OUTSTANDING)
            invites.put(Map.of(inviter, invitee), Response.OUTSTANDING);
    }

    public static void acceptInvite

    public static void cancelInvite(@NotNull Player inviter, @NotNull Player invitee) {
        invites.remove(Map.of(inviter, invitee));
    }

    private static void tick() {
        invites.entrySet().forEach(ongoingInvite -> {
            if (ongoingInvite.getValue() == Response.ACCEPTED) {

            }
        });
    }
}
