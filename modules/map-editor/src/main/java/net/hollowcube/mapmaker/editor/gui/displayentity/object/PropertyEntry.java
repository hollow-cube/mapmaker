package net.hollowcube.mapmaker.editor.gui.displayentity.object;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PropertyEntry extends View {

    public static final String SIGNAL = "property_entry.selected";

    private @Outlet("label") Label label;

    private final String property;
    private final List<String> values;
    private final String defaultValue;
    private String value;

    public PropertyEntry(
            @NotNull Context context,
            @NotNull String property,
            @NotNull String[] values,
            @NotNull String defaultValue,
            @NotNull String value
    ) {
        super(context);
        this.property = property;
        this.values = Arrays.asList(values);
        this.defaultValue = defaultValue;
        this.value = value;

        this.updateLore();
    }

    private void updateLore() {
        List<Component> lore = this.values.stream().<Component>map(value -> {
            String key = value.equals(this.value) ?
                    "gui.display_entity.properties.block_properties.entry.selected" :
                    "gui.display_entity.properties.block_properties.entry";
            return Component.translatable(key, Component.text(value));
        }).collect(Collectors.toList());

        lore.add(Component.empty());
        lore.add(Component.translatable("gui.display_entity.properties.block_properties.entry.cycle"));
        if (!this.defaultValue.equals(this.value)) {
            lore.add(Component.translatable("gui.display_entity.properties.block_properties.entry.reset"));
        }

        this.label.setComponentsDirect(
                Component.text(this.property, Style.style().decoration(TextDecoration.ITALIC, false).build()),
                lore
        );
    }

    @Action("label")
    private void handleSelect(@NotNull Player player, int slot, @NotNull ClickType type) {
        var value = switch (type) {
            case LEFT_CLICK -> this.values.get(Math.max(this.values.indexOf(this.value) + 1, 0) % this.values.size());
            case SHIFT_LEFT_CLICK -> this.defaultValue;
            default -> null;
        };
        if (value == null || value.equals(this.value)) return;

        this.value = value;
        this.performSignal(SIGNAL, this.property, this.value);
        this.updateLore();
    }


}
