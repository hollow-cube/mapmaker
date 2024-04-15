package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
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

    class NotFoundError extends RuntimeException {

    }

}
