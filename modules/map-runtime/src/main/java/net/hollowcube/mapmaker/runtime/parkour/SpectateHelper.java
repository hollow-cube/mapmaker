package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.item.ToggleFlightItem;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.util.TriState;
import net.minestom.server.codec.Codec;
import net.minestom.server.entity.Player;

import java.util.UUID;

public class SpectateHelper {
    public static final PlayState.Attachment<Boolean> SPECTATOR_FLIGHT = PlayState.attachment(Key.key("mapmaker:spectator_flight"), Codec.BOOLEAN);
    public static final PlayState.Attachment<Boolean> GAME_STATE_SAVED = PlayState.attachment(Key.key("mapmaker:game_state_saved"), Codec.BOOLEAN);

    public static void changeSpecState(Player player, TriState nextSpecState) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        var nextState = switch (world.getPlayerState(player)) {
            case ParkourState.Playing2(var saveState) when nextSpecState != TriState.FALSE ->
                    new ParkourState.Spectating(saveState);
            case ParkourState.Testing(var _, var parent) when nextSpecState != TriState.TRUE && parent != null ->
                    world.createPlayingState(parent.savedState());
            case ParkourState.Spectating(var savedState, var _) when nextSpecState != TriState.TRUE ->
                    world.createPlayingState(savedState);
            case null, default -> null;
        };

        // Must be standing on the ground (not falling) to enter spectator mode
        if (nextState instanceof ParkourState.Spectating) {
            if (world.map().getSetting(MapSettings.NO_SPECTATOR)) return;

            if (!player.isOnGround() && player.getVehicle() == null) {
                player.sendMessage(Component.translatable("map.spectator_mode.solid_ground"));
                return;
            }
        }

        if (nextState != null) world.changePlayerState(player, nextState);
    }

    public static void changeGameplaySettingsState(ParkourMapWorld world, Player player, TriState nextGameplayState) {
        var nextState = switch (world.getPlayerState(player)) {
            case ParkourState.Spectating spec when nextGameplayState != TriState.FALSE -> {
                var fakePlayState = spec.gameState();
                fakePlayState.setPos(player.getPosition());
                // Set this state to last state to create a checkpoint
                fakePlayState.setLastState(fakePlayState.copy());
                var saveState = new SaveState(UUID.randomUUID().toString(), player.getUuid().toString(), world.map().id(),
                        SaveStateType.PLAYING, PlayState.SERIALIZER, fakePlayState);
                // This is admittedly kinda hacky, we use 'saveState.getPlaytime() == 0' to determine if a save state
                // is a new state frequently. We do not want to treat fake spectator states as new states.
                saveState.setPlaytime(1);
                yield new ParkourState.Testing(saveState, spec);
            }
            case ParkourState.Testing(var _, var spec) when nextGameplayState != TriState.TRUE && spec != null -> spec;
            case null, default -> null;
        };
        if (nextState != null) world.changePlayerState(player, nextState);
    }

    public static void toggleSpectatorFlight(ParkourMapWorld world, Player player) {
        if (!(world.getPlayerState(player) instanceof ParkourState.Spectating(var _, var gameState)))
            return;

        boolean canFly = player.isAllowFlying();
        player.setAllowFlying(!canFly);
        player.setFlying(!canFly);
        gameState.set(SPECTATOR_FLIGHT, !canFly);

        // Gross
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            var id = world.itemRegistry().getItemId(inventory.getItemStack(i));
            if (id == null) continue;
            if (!canFly && id.equals(ToggleFlightItem.ID_ON.asString())) {
                inventory.setItemStack(i, ToggleFlightItem.INSTANCE_OFF.getItemStack());
            } else if (canFly && id.equals(ToggleFlightItem.ID_OFF.asString())) {
                inventory.setItemStack(i, ToggleFlightItem.INSTANCE_ON.getItemStack());
            }
        }
    }

}
