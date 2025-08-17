package net.hollowcube.mapmaker.editor.gui.displayentity;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ActionGroup;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.gui.common.anvil.ColorPickerView;
import net.hollowcube.mapmaker.gui.common.anvil.TextInputBuilder;
import net.hollowcube.mapmaker.gui.common.anvil.TextInputView;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EditTextDisplayView extends AbstractEditDisplayView<DisplayEntity.Text, TextDisplayMeta> {

    private @Outlet("option_alignment") Switch alignmentOption;
    private @Outlet("option_shadow") Switch shadowOption;
    private @Outlet("option_background") Label backgroundOption;
    private @Outlet("option_see_through") Switch seeThroughOption;

    private @Outlet("lines") Label displayLines;
    private @Outlet("text") Text displayText;

    private final TextInputBuilder<String, ?> input;

    private final List<String> lines;
    private int line = 0;


    public EditTextDisplayView(@NotNull Context context, DisplayEntity.Text display) {
        super(context, display);

        this.lines = MiniMessage.miniMessage().serialize(this.meta().getText()).lines().collect(Collectors.toList());
        this.input = TextInputView.builder()
                .icon("anvil/earth")
                .title("Text Input")
                .callback(input -> {
                    this.lines.set(this.line, input);
                    this.updateText();
                });

        this.updateState();
    }

    @Override
    protected void updateState() {
        super.updateState();

        this.alignmentOption.setOption(this.meta().getAlignment().ordinal());
        this.shadowOption.setOption(this.meta().isShadow());
        this.backgroundOption.setArgs(Component.text(String.format("#%06X", this.meta().getBackgroundColor()))
                .color(TextColor.lerp(0.2f, TextColor.color(this.meta().getBackgroundColor()), NamedTextColor.WHITE))
        );
        this.seeThroughOption.setOption(this.meta().isSeeThrough());

        this.displayText.setText(this.lines.get(this.line));
        this.displayText.setArgs(Component.text(this.lines.get(this.line)));

        List<Component> lores = new ArrayList<>();
        for (int i = 0; i < this.lines.size(); i++) {
            lores.add(Component.translatable(
                    i == this.line ? "gui.display_entity.properties.text.entry.selected" : "gui.display_entity.properties.text.entry",
                    Component.text(this.lines.get(i))
            ));
        }
        lores.add(Component.empty());
        lores.add(Component.translatable("gui.display_entity.properties.text.entry.cycle"));
        lores.add(Component.translatable("gui.display_entity.properties.text.entry.add"));

        this.displayLines.setComponentsDirect(
                Component.translatable("gui.display_entity.properties.text.header", Component.text(this.line + 1), Component.text(this.lines.size())),
                lores
        );
    }

    private void updateText() {
        this.updateState();
        this.meta().setText(MiniMessage.miniMessage().deserialize(String.join("\n", this.lines)));
    }

    // Options

    @ActionGroup("option_alignment_.*")
    public void onCycleAlignment() {
        var values = TextDisplayMeta.Alignment.values();
        this.meta().setAlignment(values[(this.meta().getAlignment().ordinal() + 1) % values.length]);
        this.updateState();
    }

    @ActionGroup("option_shadow_.*")
    public void onCycleShadow() {
        this.meta().setShadow(!this.meta().isShadow());
        this.updateState();
    }

    @Action("option_background")
    public void setBackground(Player player, int slot, ClickType type) {
        if (type == ClickType.SHIFT_LEFT_CLICK) {
            this.meta().setBackgroundColor(0x40000000);
            this.updateState();
        } else if (type == ClickType.LEFT_CLICK) {
            var builder = ColorPickerView.builder()
                    .title("Set Background Color")
                    .icon("anvil/item_frame")
                    .callback(color -> {
                        this.meta().setBackgroundColor(color.asARGB());
                        this.updateState();
                    });

            this.pushView(context -> builder.build(context, new AlphaColor(this.meta().getBackgroundColor())));
        }
    }

    @ActionGroup("option_see_through_.*")
    public void onCycleSeeThrough() {
        this.meta().setSeeThrough(!this.meta().isSeeThrough());
        this.updateState();
    }

    // Display

    @Action("lines")
    private void onLines(Player player, int slot, ClickType type) {
        if (type == ClickType.LEFT_CLICK) {
            this.line = (this.line + 1) % this.lines.size();
            this.updateState();
        } else if (type == ClickType.SHIFT_LEFT_CLICK) {
            this.line = this.lines.size();
            this.lines.add("");
            this.updateText();
            this.pushView(context -> this.input.build(context, this.lines.get(this.line)));
        }
    }

    @Action("text")
    private void onText(Player player, int slot, ClickType type) {
        if (type == ClickType.LEFT_CLICK) {
            this.pushView(context -> this.input.build(context, this.lines.get(this.line)));
        } else if (type == ClickType.SHIFT_LEFT_CLICK) {
            this.lines.remove(this.line);
            this.line = Math.min(this.line, this.lines.size() - 1);
            this.updateText();
        }
    }

}
