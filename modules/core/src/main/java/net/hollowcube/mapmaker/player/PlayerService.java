package net.hollowcube.mapmaker.player;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Blocking
public interface PlayerService {

    @NotNull DisplayName getPlayerDisplayName2(@NotNull String id);

    @NotNull String getPlayerId(@NotNull String idOrUsername);

    void updatePlayerData(@NotNull String id, @NotNull PlayerDataUpdateRequest update);

    @NotNull TabCompleteResponse getUsernameTabCompletions(@NotNull String query);

    record CreateCheckoutLinkResponse(@NotNull Link main, @Nullable Link upsell) {

        public record Link(String productId, String url) {

        }
    }

    /**
     * Creates a new checkout link for the given product.
     *
     * @param source    The source of the checkout link, eg "ingame/store" for the in-game store gui.
     * @param playerId  The ID of the player
     * @param productId The (internal) ID of the product, eg cubits_50
     */
    @NotNull CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String playerId, @NotNull String productId);

    /**
     * Creates a new checkout link for the given amount of cubits.
     *
     * @param source   The source of the checkout link, eg "ingame/prompt/upsell" for the upsell prompt when the user doesn't have enough cubits.
     * @param playerId The ID of the player
     * @param cubits   The amount of cubits
     */
    @NotNull CreateCheckoutLinkResponse createCheckoutLink(@NotNull String source, @NotNull String playerId, int cubits);


    class NotFoundError extends RuntimeException {

    }

}
