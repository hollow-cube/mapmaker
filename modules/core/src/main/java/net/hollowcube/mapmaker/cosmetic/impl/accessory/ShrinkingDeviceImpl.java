package net.hollowcube.mapmaker.cosmetic.impl.accessory;

import net.hollowcube.mapmaker.cosmetic.Cosmetic;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.session.Presence;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.attribute.AttributeModifier;
import net.minestom.server.entity.attribute.AttributeOperation;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class ShrinkingDeviceImpl extends AbstractAccessoryImpl {
    private static final TagCooldown USE_COOLDOWN = new TagCooldown("cosmetic:shrinking_device", 60 * 1000); // 1m cooldown
    private static final TaskSchedule SHRINK_DURATION = TaskSchedule.duration(45, TimeUnit.SECOND);
    private static final Sound SHRINK_SOUND = Sound.sound(SoundEvent.BLOCK_BEACON_DEACTIVATE, Sound.Source.PLAYER, 1, 2);
    private static final Sound UNSHRINK_SOUND = Sound.sound(SoundEvent.BLOCK_BEACON_ACTIVATE, Sound.Source.PLAYER, 1, 2);
    private static final Component COOLDOWN_MESSAGE = Component.translatable("cosmetic.accessory.shrinking_device.cooldown");

    private static final AttributeModifier SHRINK_MODIFIER = new AttributeModifier("cosmetic:shrinking_device", -0.75, AttributeOperation.ADD_VALUE);

    public ShrinkingDeviceImpl(@NotNull Cosmetic cosmetic) {
        super(cosmetic);
    }

    @Override
    public void useItem(@NotNull Player player) {
        var playerId = PlayerDataV2.fromPlayer(player).id();
        var session = SessionManager.instance.getSession(playerId);
        if (session == null || session.presence() == null) return;

        // Do NOT, under ANY circumstance, run this if the player is not in the hub.
        if (!Presence.TYPE_MAPMAKER_HUB.equals(session.presence().type()))
            return;

        // If they are already shrunk then unshrink them immediately.
        var scaleAttribute = player.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttribute.removeModifier(SHRINK_MODIFIER) != null) {
            player.getInstance().playSound(UNSHRINK_SOUND, player.getPosition());
            return;
        }

        if (!USE_COOLDOWN.test(player)) {
            player.sendMessage(COOLDOWN_MESSAGE);
            return;
        }

        scaleAttribute.addModifier(SHRINK_MODIFIER);
        player.getInstance().playSound(SHRINK_SOUND, player.getPosition());

        player.scheduler()
                .buildTask(() -> {
                    if (scaleAttribute.removeModifier(SHRINK_MODIFIER) != null) {
                        player.getInstance().playSound(UNSHRINK_SOUND, player.getPosition());
                    }
                })
                .delay(SHRINK_DURATION).schedule();
    }
}
