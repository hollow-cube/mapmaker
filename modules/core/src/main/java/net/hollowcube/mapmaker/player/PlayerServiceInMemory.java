package net.hollowcube.mapmaker.player;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.responses.PlayerAlts;
import net.hollowcube.mapmaker.player.responses.TotpSetupResponse;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/// A (mostly) functional player service with all data stored in memory.
///
/// Unsupported operations will throw "not implemented", but should be implemented if needed.
@NotNullByDefault
public class PlayerServiceInMemory implements PlayerService {

    @Override
    public DisplayName getPlayerDisplayName2(String id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getPlayerId(String idOrUsername) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void updatePlayerData(String id, PlayerDataUpdateRequest update) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Set<String> getUnlockedCosmetics(String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void buyCosmetic(String id, Cosmetic cosmetic, @Nullable Integer coins, @Nullable Integer cubits, @Nullable JsonObject items) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void buyUpgrade(String playerId, String upgradeId, int cubits, JsonObject meta) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public JsonObject getPlayerBackpack(String id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public TabCompleteResponse getUsernameTabCompletions(String query) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public CreateCheckoutLinkResponse createCheckoutLink(String source, String username, String product) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable HypercubeStatus getHypercubeStatus(String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public LinkResult attemptVerify(String playerId, String secret) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public TotpResult checkTotp(String playerId, @Nullable String code) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public TotpResult removeTotp(String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public @Nullable TotpSetupResponse beginTotpSetup(String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public TotpResult completeTotpSetup(String playerId, String code) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<PlayerAlts.Alt> getAlts(String playerId) {
        throw new UnsupportedOperationException("not implemented");
    }

}
