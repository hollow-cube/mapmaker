package net.hollowcube.mapmaker.runtime.parkour.action.impl;

import net.hollowcube.mapmaker.panels.Sprite;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.editors.PlaySoundEditor;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.utils.MathUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public record PlaySoundAction(
    @Nullable SoundEvent event,
    float volume
) implements Action {

    private static final Sprite SPRITE = new Sprite("icon2/1_1/music_disc", 1, 1);

    public static final Key KEY = Key.key("mapmaker:play_sound");
    public static final StructCodec<PlaySoundAction> CODEC = StructCodec.struct(
        "event", SoundEvent.CODEC.optional(), PlaySoundAction::event,
        "volume", StructCodec.FLOAT.optional(1f), PlaySoundAction::volume,
        PlaySoundAction::new
    );
    public static final Editor<PlaySoundAction> EDITOR = new Editor<>(
        PlaySoundEditor::new, _ -> SPRITE,
        PlaySoundEditor::makeThumbnail, Set.of()
    );

    public PlaySoundAction withEvent(SoundEvent event) {
        return new PlaySoundAction(event, this.volume);
    }

    public PlaySoundAction withVolume(int volume) {
        return new PlaySoundAction(this.event, MathUtils.clamp(((float) volume) / 100f, 0f, 1f));
    }

    @Override
    public StructCodec<? extends Action> codec() {
        return CODEC;
    }

    @Override
    public void applyTo(Player player, PlayState state) {
        if (this.event == null) return;

        player.stopSound(SoundStop.named(this.event));
        player.playSound(Sound.sound(this.event, Sound.Source.MASTER, this.volume, 1f));
    }

}
