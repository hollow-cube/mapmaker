package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Blocking
public interface PlayerService {

    @NotNull
    DisplayName getPlayerDisplayName2(@NotNull String id);

    @NotNull
    String getPlayerId(@NotNull String idOrUsername);

    void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update);

    /**
     * Returns the unlocked cosmetics for the given player by their path, eg `head/crown`.
     */
    @NotNull
    Set<String> getUnlockedCosmetics(@NotNull String playerId);

    void buyCosmetic(@NotNull String id, @NotNull Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits, @Nullable JsonObject items);

    void buyUpgrade(@NotNull String playerId, @NotNull String upgradeId, int cubits, @NotNull JsonObject meta);

    @NotNull
    JsonObject getPlayerBackpack(@NotNull String id);

    @NotNull
    TabCompleteResponse getUsernameTabCompletions(@NotNull String query);

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
    CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String username, @NotNull String product);

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
        VALID_CODE,
        INVALID_FORMAT,
        INVALID_CODE,
        NOT_ENABLED,
    }

    @NotNull TotpResult checkTotp(@NotNull String playerId, @NotNull String code);

    @Nullable TotpSetupResponse beginTotpSetup(@NotNull String playerId);

    enum TotpSetupResult {
        COMPLETED,
        INVALID_FORMAT,
        INVALID_CODE,
        NOT_STARTED,
        ALREADY_ENABLED,
    }

    @NotNull TotpSetupResult completeTotpSetup(@NotNull String playerId, @NotNull String code);

    class NotFoundError extends RuntimeException {

    }

}
