package net.hollowcube.mapmaker.map.script.api.entity;

import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.error.LuaArgError;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.script.api.LuaEventSource;
import net.hollowcube.mapmaker.map.script.api.math.LuaCuboid;
import net.hollowcube.mapmaker.map.script.api.world.LuaWorldView;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.ParticlePacket;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
@LuaObject
public class LuaPlayer extends LuaEntity {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Player player;
    private LuaWorldView lazyWorld = null;

    private LuaEventSource<Callbacks.UseItem> useItem;

    public LuaPlayer(@NotNull Player player) {
        super(player);
        this.player = player;
    }

    @LuaProperty
    public @NotNull LuaWorldView getWorld() {
        if (lazyWorld == null) {
            lazyWorld = new LuaWorldView(player);
        }
        return lazyWorld;
    }

    @LuaProperty
    public @NotNull Point getPosition() {
        return player.getPosition();
    }

    @LuaProperty
    public @NotNull LuaCuboid getBoundingBox() {
        return new LuaCuboid(player);
    }

    @LuaProperty
    public @NotNull LuaEventSource<Callbacks.UseItem> getUseItem() {
        if (useItem == null) {
            useItem = LuaEventSource.create(
                    Callbacks.UseItem.class, PlayerUseItemEvent.class,
                    (_, useItem) -> useItem.call()
//                    (e, useItem) -> useItem.call(e.getHand() == Player.Hand.MAIN ? e.getPlayer().getHeldSlot() : PlayerInventoryUtils.OFFHAND_SLOT)
            );
        }
        return useItem;
    }

    @LuaMethod
    public void sendMessage(@NotNull String message) {
        player.sendMessage(MM.deserialize(message));
    }

    @LuaMethod
    public void showTitle(@Nullable String title, @Nullable String subtitle, @Nullable Integer fadeIn, @Nullable Integer stay, @Nullable Integer fadeOut) {
        if (title == null && subtitle == null) return;
        var titleComp = title == null ? Component.empty() : MM.deserialize(title);
        var subtitleComp = subtitle == null ? Component.empty() : MM.deserialize(subtitle);
        var times = Title.Times.times(
                fadeIn != null ? Ticks.duration(fadeIn) : Ticks.duration(10L),
                stay != null ? Ticks.duration(stay) : Ticks.duration(70L),
                fadeOut != null ? Ticks.duration(fadeOut) : Ticks.duration(20L)
        );
        player.showTitle(Title.title(titleComp, subtitleComp, times));
    }

    @LuaMethod
    public void teleport(@NotNull Point pos, @Nullable Double yaw, @Nullable Double pitch, @Nullable String relativeFlags) {
        int relFlags = 0;

        // If yaw or pitch is unspecified, default to zero and relative 0.
        final float targetYaw;
        if (yaw != null) {
            targetYaw = yaw.floatValue();
        } else {
            targetYaw = 0;
            relFlags |= RelativeFlags.YAW;
        }
        final float targetPitch;
        if (pitch != null) {
            targetPitch = pitch.floatValue();
        } else {
            targetPitch = 0;
            relFlags |= RelativeFlags.PITCH;
        }
        final Pos targetPos = new Pos(pos, targetYaw, targetPitch);

        if (relativeFlags != null) {
            for (char c : relativeFlags.toCharArray()) {
                int flag = switch (c) {
                    case 'x' -> RelativeFlags.X;
                    case 'y' -> RelativeFlags.Y;
                    case 'z' -> RelativeFlags.Z;
                    case 'r' -> RelativeFlags.YAW;
                    case 'p' -> RelativeFlags.PITCH;
                    default -> throw new LuaArgError(3, "Unknown relative teleport flag: " + c);
                };
                relFlags |= flag;
            }
        }

        // todo ensure the target position is inside the world border (including bounding box)
        player.teleport(targetPos, null, relFlags);
    }

    @LuaMethod
    public void playSound(@NotNull String sound, @Nullable Point pos, @Nullable Double volume, @Nullable Double pitch) {
        var soundEvent = SoundEvent.fromNamespaceId(sound);
        if (soundEvent == null) throw new LuaArgError(0, "Invalid sound name");

        //todo source as arg
        var s = Sound.sound(soundEvent, Sound.Source.MASTER, (float) orElse(volume, 1.0), (float) orElse(pitch, 1.0));
        if (pos != null) player.playSound(s, pos);
        else player.playSound(s);
    }

    @LuaMethod
    public void spawnParticle(@NotNull Particle particle, @NotNull Point pos, @NotNull Point delta, double speed, int count, @Nullable Boolean forceVisible) {
        boolean longDistance = forceVisible != null && forceVisible;
        player.sendPacket(new ParticlePacket(particle, longDistance, pos, delta, (float) speed, count));
    }

    @LuaMethod
    public void reset(@Nullable Boolean toStart) {
        boolean resetToStart = toStart != null && toStart;

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        world.callEvent(new MapPlayerResetEvent(player, world, !resetToStart));
    }

    @LuaMethod
    public void completeMap() {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        world.callEvent(new MapPlayerCompleteMapEvent(player, world, "scripted"));
    }

    @LuaMethod
    public void giveItem() {
        player.getInventory().setItemStack(3, ItemStack.of(Material.STICK));
    }

    public static final class Callbacks {

        @LuaBindable
        public interface UseItem {
            void call();
        }

    }

    private double orElse(@Nullable Double d, double def) {
        return d == null ? def : d;
    }
}
