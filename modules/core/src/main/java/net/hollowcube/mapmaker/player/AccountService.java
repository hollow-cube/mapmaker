package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Blocking
public interface AccountService {

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

    @RuntimeGson
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

    class NotFoundError extends RuntimeException {}
}
