package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.responses.PlayerAlts;
import net.hollowcube.mapmaker.player.responses.SendFriendRequestResult;
import net.hollowcube.mapmaker.player.responses.PlayerNotificationResponse;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import net.hollowcube.mapmaker.util.AbstractHttpService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@Blocking
public interface PlayerService {
    int DEFAULT_TAB_COMPLETE_LIMIT = 25;

    @NotNull
    DisplayName getPlayerDisplayName2(@NotNull String id);

    default @NotNull List<DisplayName> getPlayerDisplayNames(@NotNull List<String> ids) {
        return ids.stream() // todo probably makes sense to add a bulk endpoint
            .map(this::getPlayerDisplayName2)
            .toList();
    }

    @NotNull
    String getPlayerId(@NotNull String idOrUsername);

    @NotNull PlayerData getPlayerData(@NotNull String id);

    void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update);

    /**
     * Returns the unlocked cosmetics for the given player by their path, eg `head/crown`.
     */
    @NotNull
    Set<String> getUnlockedCosmetics(@NotNull String playerId);

    void buyCosmetic(
        @NotNull String id, @NotNull Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits,
        @Nullable JsonObject items
    );

    void buyUpgrade(@NotNull String playerId, @NotNull String upgradeId, int cubits, @NotNull JsonObject meta);

    @NotNull
    JsonObject getPlayerBackpack(@NotNull String id);

    @NotNull
    TabCompleteResponse getUsernameTabCompletions(@NotNull String query, int limit);

    default @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query) {
        return this.getUsernameTabCompletions(query, DEFAULT_TAB_COMPLETE_LIMIT);
    }

    record CreateCheckoutLinkResponse(@NotNull String url) {
    }

    /**
     * Creates a new checkout link for the given product.
     *
     * @param source   The source of the checkout link, eg "ingame/store" for the in-game store gui.
     * @param username The username of the player
     * @param product  The (internal) ID of the product, eg cubits_50
     */
    @NotNull
    CreateCheckoutLinkResponse createCheckoutLink(
        @NotNull String source, @NotNull String username, @NotNull String product);

    @Nullable HypercubeStatus getHypercubeStatus(@NotNull String playerId);

    enum LinkResult {
        SUCCESS,
        INVALID_SECRET,
        EXPIRED_SECRET,
        ALREADY_LINKED,
        INTERNAL_ERROR
    }

    @NotNull LinkResult attemptVerify(@NotNull String playerId, @NotNull String secret);

    enum TotpResult {
        SUCCESS,
        INVALID_FORMAT,
        INVALID_CODE,
        NOT_ENABLED,
        ALREADY_ENABLED,
    }

    @NotNull TotpResult checkTotp(@NotNull String playerId, @Nullable String code);

    @NotNull TotpResult removeTotp(@NotNull String playerId);

    @Nullable TotpSetupResponse beginTotpSetup(@NotNull String playerId);

    @NotNull TotpResult completeTotpSetup(@NotNull String playerId, @NotNull String code);

    @NotNull List<PlayerAlts.Alt> getAlts(@NotNull String playerId);

    // Friendships

    default @NotNull Page<PlayerFriend> getPlayerFriends(@NotNull String playerId, @NotNull Pageable pageable) {
        return getPlayerFriends(playerId, null, pageable);
    };

    @NotNull Page<PlayerFriend> getPlayerFriends(@NotNull String playerId, @Nullable Boolean onlineState, @NotNull Pageable pageable);

    void removeFriend(@NotNull String playerId, @NotNull String targetId);

    @NotNull Page<FriendRequest> getFriendRequests(
        @NotNull String playerId, boolean incoming, @NotNull Pageable pageable);

    /**
     * Sends a friend request or accepts a friend request if an inverted request already exists (one from the target)
     *
     * @param playerId player that is sending the request
     * @param targetId player that is receiving the request
     */
    @NotNull SendFriendRequestResult sendFriendRequest(@NotNull String playerId, @NotNull String targetId);

    @NotNull FriendRequest deleteFriendRequest(
        @NotNull String playerId, @NotNull String targetId, boolean bidirectional);

    // Blocks

    void blockPlayer(@NotNull String playerId, @NotNull String targetId);

    @NotNull Page<BlockedPlayer> getBlockedPlayers(@NotNull String playerId, @NotNull Pageable pageable);

    void unblockPlayer(@NotNull String playerId, @NotNull String targetId);

    List<BlockedPlayer> getBlocksBetween(@NotNull String playerId, @NotNull String targetId, boolean bidirectional);

    /**
     *
     * @param player        player to test the block for
     * @param targetId      target to test the block against
     * @param bidirectional if bidirectional or not
     * @return true if the player is blocked by the target - you should stop execution
     */
    default boolean failIfBlocked(
        @NotNull Player player, @NotNull String targetId, @NotNull String targetUsername, boolean bidirectional) {
        var blocks = this.getBlocksBetween(targetId, player.getUuid().toString(), bidirectional);
        if (blocks.isEmpty()) return false;

        BlockedPlayer block = blocks.getFirst();
        if (block.playerId().equals(player.getUuid().toString())) { // blocked by target
            player.sendMessage(
                Component.translatable("generic.command.blocked_by_target", Component.text(targetUsername)));
        } else { // blocked by self
            player.sendMessage(Component.translatable("generic.command.blocked_by_self", Component.text(targetUsername)));
        }
        return true;
    }

    // Notifications
    @NotNull PlayerNotificationResponse getNotifications(@NotNull String playerId, int page, boolean unread);
    void deleteNotification(@NotNull String playerId, @NotNull String notificationId);
    void markNotificationRead(@NotNull String playerId, @NotNull String notificationId, boolean read);
    void createNotification(
        @NotNull String playerId, @NotNull String type, @NotNull String key, @Nullable JsonObject data,
        @Nullable Integer expiresInSeconds, boolean replaceUnread
    );

    // Recap

    @Nullable String getRecap(@NotNull String playerId, int year);

    class BadRequestError extends RuntimeException {}

    class NotFoundError extends RuntimeException {

    }

    class AlreadyExistsError extends RuntimeException {

    }

    record Pageable(int page, int pageSize) {}

    @RuntimeGson
    record Page<T>(int page, int totalItems, @NotNull List<T> items) {

        public static <T> Page<T> fromJson(@NotNull String json, @NotNull Class<T> clazz) {
            return AbstractHttpService.GSON.fromJson(json, TypeToken.getParameterized(Page.class, clazz).getType());
        }

        public static <T> Page<T> empty() {
            return new Page<>(0, 0, List.of());
        }

    }

}
