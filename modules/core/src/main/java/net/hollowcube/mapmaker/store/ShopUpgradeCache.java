package net.hollowcube.mapmaker.store;

import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.function.Predicate;

/**
 * <p>Caches which packages the player has unlocked (either directly or through hypercube).</p>
 *
 * <p>While writing this class i realize its kinda gross, but idk im tired and it will work for now.</p>
 */
public final class ShopUpgradeCache {
    private static final EnumMap<ShopUpgrade, Predicate<String>> directEntries = new EnumMap<>(ShopUpgrade.class);
    private static final EnumMap<ShopUpgrade, Predicate<String>> indirectEntries = new EnumMap<>(ShopUpgrade.class);

    public static void init(@NotNull PermManager permManager) {
        for (ShopUpgrade upgrade : ShopUpgrade.values()) {
            directEntries.put(upgrade, permManager.createPrefetchedCondition(upgrade.directPerm()));
            indirectEntries.put(upgrade, permManager.createPrefetchedCondition(upgrade.indirectPerm()));
        }
    }

    /**
     * Returns true if the player has the upgrade, false otherwise. The value is always prefetched and never
     * results in a network call.
     *
     * @param direct if true, only checks if the player has the upgrade directly (ie ignoring hypercube)
     */
    public static boolean has(@NotNull Player player, @NotNull ShopUpgrade upgrade, boolean direct) {
        EnumMap<ShopUpgrade, Predicate<String>> entries = direct ? directEntries : indirectEntries;
        return entries.getOrDefault(upgrade, _ -> false).test(PlayerData.fromPlayer(player).id());
    }

    /**
     * Returns true if the player has the upgrade, false otherwise. The value is always prefetched and never
     * results in a network call.
     *
     * @param direct if true, only checks if the player has the upgrade directly (ie ignoring hypercube)
     */
    public static boolean has(@NotNull String playerId, @NotNull ShopUpgrade upgrade, boolean direct) {
        EnumMap<ShopUpgrade, Predicate<String>> entries = direct ? directEntries : indirectEntries;
        return entries.getOrDefault(upgrade, $ -> false).test(playerId);
    }
}
