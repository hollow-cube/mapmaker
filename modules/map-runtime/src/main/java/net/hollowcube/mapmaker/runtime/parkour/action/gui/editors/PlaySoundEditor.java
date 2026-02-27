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
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PlaySoundEditor extends AbstractActionEditorPanel<@NotNull PlaySoundAction> {

    private final ControlledNumberInput volumeInput;
    private final Text soundInput;

    public PlaySoundEditor(ActionList.Ref ref) {
        super(ref);

        background("action/editor/container", -10, -31);

        add(1, 1, AbstractActionEditorPanel.groupText(7, "Sound").translationKey("gui.action.play_sound.sound"));
        this.soundInput = add(1, 2, new Text("gui.action.play_sound.sound", 7, 1, ""));
        this.soundInput.align(6, 5);
        this.soundInput.background("generic2/input/7_1_shadow");
        this.soundInput.lorePostfix(AbstractActionEditorPanel.LORE_POSTFIX_CLICKEDIT);
        this.soundInput.onLeftClick(() ->
            host.pushView(new AnvilSearchView<>(
                "action/anvil/search_icon", "Search Sounds",
                Autocompletors::searchSounds,
                PlaySoundEditor::makeSoundButton,
                update(PlaySoundAction::withEvent)
            ))
        );

        this.volumeInput = add(1, 3, new ControlledNumberInput("play_sound.volume", update(PlaySoundAction::withVolume)));
        this.volumeInput.range(1, 100);
        this.volumeInput.stepped(5, 10);
    }

    @Override
    protected void update(PlaySoundAction data) {
        this.volumeInput.update((int) (data.volume() * 100));
        if (data.event() == null) {
            this.soundInput.text("");
        } else {
            var name = data.event().key().asMinimalString();
            this.soundInput.text(FontUtil.shorten(name, 115, 5));
        }
    }

    private static Button makeSoundButton(SoundEvent sound) {
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
            .text(Component.text(sound.name()), List.of());
    }

    public static TranslatableComponent makeThumbnail(@Nullable PlaySoundAction action) {
        if (action == null || action.event() == null)
            return Component.translatable("gui.action.play_sound.thumbnail.empty");
        return Component.translatable("gui.action.play_sound.thumbnail", List.of(
            Component.text(action.event().key().asMinimalString()),
            TranslationArgument.numeric((int) (action.volume() * 100))
        ));
    }
}
