package net.hollowcube.mapmaker.runtime.parkour.action.gui.editors;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.panels.AnvilSearchView;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.AbstractActionEditorPanel;
import net.hollowcube.mapmaker.runtime.parkour.action.gui.ControlledNumberInput;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.PlaySoundAction;
import net.hollowcube.mapmaker.util.Autocompletors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.LORE_POSTFIX_CLICKEDIT;

public class PlaySoundEditor extends AbstractActionEditorPanel<@NotNull PlaySoundAction> {

    private final Text soundInput;
    private final ControlledNumberInput volumeInput;
    private final ControlledNumberInput pitchInput;

    public PlaySoundEditor(ActionList.Ref ref) {
        super(ref);

        background("action/editor/container", -10, -31);

        add(1, 1, AbstractActionEditorPanel.groupText(7, "Sound").translationKey("gui.action.play_sound.sound"));
        this.soundInput = add(1, 2, new Text("gui.action.play_sound.sound", 7, 1, ""));
        this.soundInput.align(6, 5);
        this.soundInput.background("generic2/input/7_1_shadow");
        this.soundInput.lorePostfix(LORE_POSTFIX_CLICKEDIT);
        this.soundInput.onLeftClick(() ->
            host.pushView(AnvilSearchView.simple(
                "action/anvil/search_icon", "Search Sounds",
                Autocompletors::searchSounds,
                PlaySoundEditor::makeSoundButton,
                update(PlaySoundAction::withEvent)
            ))
        );

        this.volumeInput = add(1, 3, new ControlledNumberInput(4, "play_sound.volume", update(PlaySoundAction::withVolume), false, true));
        this.volumeInput.range(1, 100);
        this.volumeInput.stepped(5, 10);

        this.pitchInput = add(6, 3, new ControlledNumberInput(2, "play_sound.pitch",
            update((v, i) -> v.withPitch(i / 100f)), false, false));
        this.pitchInput.parsed(i -> String.valueOf(i / 100f), s -> (int) (Float.parseFloat(s) * 100));
        this.pitchInput.formatted(i -> String.valueOf(i / 100f));
        this.pitchInput.range(0, 200);
    }

    @Override
    protected void update(PlaySoundAction data) {
        if (data.event() == null) {
            this.soundInput.text("");
        } else {
            var name = data.event().key().asMinimalString();
            this.soundInput.text(FontUtil.shorten(name, 115, 5));
        }
        this.volumeInput.update((int) (data.volume() * 100));
        this.pitchInput.update((int) (data.pitch() * 100));
    }

    public static Button makeSoundButton(@Nullable SoundEvent sound) {
        if (sound == null) {
            return new Button(null, 1, 1)
                .from(ItemStack.of(Material.BARRIER))
                .text(Component.text("All").decoration(TextDecoration.ITALIC, false), List.of());
        }

        var id = sound.key().asMinimalString();

        var key = id.substring(0, id.indexOf('.'));
        var path = id.indexOf('.', key.length() + 1) != -1 ?
            id.substring(key.length() + 1, id.indexOf('.', key.length() + 1)) :
            id.substring(key.length() + 1);
        var icon = switch (key) {
            case "block" -> OpUtils.mapOr(Block.fromKey(path), it -> it.registry().material(), Material.BARRIER);
            case "entity" -> {
                if (path.equals("player")) {
                    yield Material.PLAYER_HEAD;
                } else {
                    yield OpUtils.firstNonNull(
                        Material.fromKey(path + "_spawn_egg"),
                        Material.fromKey(path),
                        Material.BLUE_EGG
                    );
                }
            }
            case "item" -> OpUtils.firstNonNull(Material.fromKey(path), Material.BARRIER);
            case "music_disc" -> OpUtils.firstNonNull(Material.fromKey("music_disc_" + path), Material.BARRIER);
            case "music" -> Material.JUKEBOX;
            case "ambient" -> Material.FIREFLY_BUSH;
            default -> Material.BARRIER;
        };

        return new Button(null, 1, 1)
            .from(ItemStack.of(icon))
            .text(Component.text(sound.name()).decoration(TextDecoration.ITALIC, false), List.of());
    }

    public static TranslatableComponent makeThumbnail(@Nullable PlaySoundAction action) {
        if (action == null || action.event() == null)
            return Component.translatable("gui.action.play_sound.thumbnail.empty");
        return Component.translatable("gui.action.play_sound.thumbnail", List.of(
            Component.text(action.event().key().asMinimalString()),
            TranslationArgument.numeric((int) (action.volume() * 100)),
            TranslationArgument.numeric(action.pitch())
        ));
    }
}
