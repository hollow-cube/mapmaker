package net.hollowcube.mapmaker.hub;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.hub.feature.misc.DoubleJumpFeature;
import net.hollowcube.mapmaker.hub.item.*;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.PlayerState;
import net.hollowcube.mapmaker.misc.BossBars;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.List;

public sealed interface HubPlayerState extends PlayerState<HubPlayerState, HubMapWorld> {

    record Default() implements HubPlayerState {
        private static final AttributeModifier REACH_MOD = new AttributeModifier("mapmaker:hub_reach",
                40 - Attribute.ENTITY_INTERACTION_RANGE.defaultValue(), AttributeOperation.ADD_VALUE);

        private static final List<BossBar> BOSS_BARS = List.of(
                BossBars.createLine1(Component.text(FontUtil.rewrite("bossbar_ascii_1", "Map Maker Early Access"), TextColor.color(0x3895FF))),
                BossBars.ADDRESS_LINE
        );

        @Override
        public void configurePlayer(HubMapWorld world, Player player, @Nullable HubPlayerState lastState) {
            HubPlayerState.super.configurePlayer(world, player, lastState);

            var playerData = PlayerData.fromPlayer(player);
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlying(true);
            player.setFlyingSpeed(player.getTag(DoubleJumpFeature.TAG) ? 0 : 0.05f);
            player.setHeldItemSlot(playerData.getSetting(PlayerSettings.HUB_SELECTED_SLOT).byteValue());
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).addModifier(REACH_MOD);

            world.itemRegistry().setItemStack(player, PlayMapsItem.ID, 0);
            world.itemRegistry().setItemStack(player, CreateMapsItem.ID, 1);
            if (CoreFeatureFlags.ORGANIZATIONS.test(player))
                world.itemRegistry().setItemStack(player, OrgMapsItem.ID, 2);
            world.itemRegistry().setItemStack(player, OpenNotificationsItem.ID, 4);
            world.itemRegistry().setItemStack(player, OpenStoreItem.ID, 7);
            world.itemRegistry().setItemStack(player, OpenCosmeticsMenuItem.ID, 8);

            OpenNotificationsItem.checkForUnread(world, new WeakReference<>(player));

            BOSS_BARS.forEach(player::showBossBar);

            if (player instanceof MapPlayer mp) {
                mp.setCanSendPose(false);
            }
        }

        @Override
        public void resetPlayer(HubMapWorld world, Player player, @Nullable HubPlayerState nextState) {
            player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE).removeModifier(REACH_MOD);
            BossBars.clear(player);

            // Write their settings to the database
            var playerData = PlayerData.fromPlayer(player);
            FutureUtil.submitVirtual(() -> playerData.writeUpdatesUpstream(world.server().playerService()));
        }
    }

}
