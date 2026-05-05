package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.StopSoundEditor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record StopSoundAction(@Nullable SoundEvent event) implements Action {

    private static final Sprite SPRITE = new Sprite("icon2/1_1/volume_x", 1, 1);

    public static final Key KEY = Key.key("mapmaker:stop_sound");
    public static final StructCodec<StopSoundAction> CODEC = StructCodec.struct(
        "event", SoundEvent.CODEC.optional(), StopSoundAction::event,
        StopSoundAction::new
    );
    public static final Editor<StopSoundAction> EDITOR = new Editor<>(
        StopSoundEditor::open, _ -> SPRITE,
        StopSoundEditor::makeThumbnail, Set.of()
    );

    public StopSoundAction withEvent(SoundEvent event) {
        return new StopSoundAction(event);
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        if (this.event != null) {
            player.stopSound(SoundStop.named(this.event));
        } else {
            player.stopSound(SoundStop.source(Sound.Source.VOICE));
            player.stopSound(SoundStop.source(Sound.Source.RECORD));
        }
    }

}
