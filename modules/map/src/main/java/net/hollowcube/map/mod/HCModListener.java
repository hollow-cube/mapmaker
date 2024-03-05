package net.hollowcube.map.mod;

import net.minestom.server.event.player.PlayerPluginMessageEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class HCModListener {

    public void handlePluginMessage(@NotNull PlayerPluginMessageEvent event) {
        if (!event.getIdentifier().startsWith("hollowcube:")) return;

//        var player = event.getPlayer();
//        switch (HCMod.readPlayPacket(event)) {
//            case HCClientModifyAnimationPacket packet -> {
//                var animation = AnimationBuilder.instance;
//                if (animation == null) return;
//
//                if (packet.currentTick() != null) {
//                    animation.seek(packet.currentTick(), true);
//                    player.sendPacket(new HCAckAnimationChangePacket(packet.sequence()));
//                }
//
//                if (packet.playing() != null) {
//                    if (packet.playing()) animation.play();
//                    else animation.pause();
//                }
//            }
//            case null, default -> System.out.println("OOGA BOOGA UNHANDLED PACKET TYPE! " + event.getIdentifier());
//        }
    }
}
