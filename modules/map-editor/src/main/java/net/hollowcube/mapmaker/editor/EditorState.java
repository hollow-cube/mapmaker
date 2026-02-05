package net.hollowcube.mapmaker.editor;

import net.hollowcube.compat.axiom.AxiomPlayer;
import net.hollowcube.compat.noxesium.components.NoxesiumGameComponents;
import net.hollowcube.compat.noxesium.handshake.NoxesiumPlayer;
import net.hollowcube.mapmaker.editor.item.BuilderMenuItem;
import net.hollowcube.mapmaker.editor.item.ExitTestModeItem;
import net.hollowcube.mapmaker.editor.vanilla.DisplayEntityEditor;
import net.hollowcube.mapmaker.map.PlayerState;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.runtime.parkour.item.ResetSaveStateItem;
import net.hollowcube.mapmaker.runtime.parkour.item.ReturnToCheckpointItem;
import net.hollowcube.mapmaker.runtime.parkour.item.SetSpectatorCheckpointItem;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

import static net.hollowcube.mapmaker.runtime.parkour.ParkourState.AnyPlaying.resetTeleport;

public sealed interface EditorState extends PlayerState<EditorState, EditorMapWorld> {

    record Building(SaveState saveState) implements EditorState {

        @Override
        public void configurePlayer(EditorMapWorld world, Player player, @Nullable EditorState lastState) {
            EditorState.super.configurePlayer(world, player, lastState);
            player.setGameMode(GameMode.CREATIVE);
            player.setPermissionLevel(4);

            var noxesium = NoxesiumPlayer.get(player);
            noxesium.set(NoxesiumGameComponents.HELD_ITEM_NAME_OFFSET, 14);
            noxesium.set(NoxesiumGameComponents.CUSTOM_CREATIVE_ITEMS, world.itemRegistry().getCustomPublicItemStacks());

            if (lastState == null)
                saveState.setPlayStartTime(System.currentTimeMillis());

            AxiomPlayer.setEnabled(player, true);

            var editState = saveState.state(EditState.class);
            editState.inventory().forEach(player.getInventory()::setItemStack);
            player.setHeldItemSlot((byte) editState.selectedSlot());
            player.setFlying(editState.isFlying());
            MiscFunctionality.applyCosmetics(player, PlayerData.fromPlayer(player));

            if (editState.pos() == null) {
                // If there is no position stored then this is a fresh edit state so add the builder menu
                var itemStack = world.itemRegistry().getItemStack(BuilderMenuItem.ID, null);
                player.getInventory().addItemStack(itemStack);
            }

            if (lastState != null) {
                resetTeleport(player, Objects.requireNonNullElseGet(editState.pos(),
                        () -> world.map().settings().getSpawnPoint()));
            }
        }

        @Override
        public void resetPlayer(EditorMapWorld world, Player player, @Nullable EditorState nextState) {
            DisplayEntityEditor.clear(player);

            AxiomPlayer.setEnabled(player, false);

            var editState = saveState.state(EditState.class);
            editState.setPos(player.getPosition());
            editState.setFlying(player.isFlying());
            var inventory = new HashMap<Integer, ItemStack>();
            for (int i = 0; i < player.getInventory().getInnerSize(); i++) {
                var itemStack = player.getInventory().getItemStack(i);
                if (!itemStack.isAir()) inventory.put(i, itemStack);
            }
            editState.setInventory(inventory);
            editState.setSelectedSlot(player.getHeldSlot());

            // Note that unlike parkour worlds, we only save when the player leaves the world.
        }
    }

    record Testing(SaveState editState) implements EditorState {

        @Override
        public void configurePlayer(EditorMapWorld world, Player player, @Nullable EditorState lastState) {
            EditorState.super.configurePlayer(world, player, lastState);

            // This is a pretty gross line, but oh well for now i guess.
            world.testWorldOrCreate(testWorld -> testWorld.addPlayerDirect(player, () -> {
                testWorld.itemRegistry().setItemStack(player, ReturnToCheckpointItem.ID, 0);
                testWorld.itemRegistry().setItemStack(player, SetSpectatorCheckpointItem.ID, 1);
                // 3,4,5 = items
                testWorld.itemRegistry().setItemStack(player, ResetSaveStateItem.ID, 7);
                testWorld.itemRegistry().setItemStack(player, ExitTestModeItem.ID, 8);
            }));
        }

        @Override
        public void resetPlayer(EditorMapWorld world, Player player, @Nullable EditorState nextState) {
            var testWorld = Objects.requireNonNull(world.testWorld(), "missing test world");

            testWorld.removePlayer(player);
        }
    }

}
