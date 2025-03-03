package net.hollowcube.mapmaker.map.gui.displayentity.object;

import com.mojang.datafixers.util.Pair;
import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponent;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ComponentEntry extends View {

    public static final String SIGNAL = "component_entry.selected";

    private @Outlet("label") Label label;

    private final DataComponent<?> component;
    private final List<Pair<?, Component>> values;
    private final Object defaultValue;
    private Object value;

    public ComponentEntry(
            @NotNull Context context,
            @NotNull DataComponent<?> component,
            @NotNull List<Pair<?, Component>> values,
            @Nullable Object defaultValue,
            @Nullable Object value
    ) {
        super(context);
        this.component = component;
        this.values = values;
        this.defaultValue = defaultValue;
        this.value = value;

        this.updateLore();
    }

    private void updateLore() {
        List<Component> lore = this.values.stream().<Component>map(entry -> {
            String key = Objects.equals(entry.getFirst(), this.value) ?
                    "gui.display_entity.properties.item_components.entry.selected" :
                    "gui.display_entity.properties.item_components.entry";
            return Component.translatable(key, entry.getSecond());
        }).collect(Collectors.toList());

        lore.add(Component.empty());
        lore.add(Component.translatable("gui.display_entity.properties.block_properties.entry.cycle"));
        if (!Objects.equals(this.defaultValue, this.value)) {
            lore.add(Component.translatable("gui.display_entity.properties.block_properties.entry.reset"));
        }

        this.label.setComponentsDirect(
                Component.translatable("gui.display_entity.properties.item_components.type.%s.%s.name".formatted(
                        this.component.key().namespace(),
                        this.component.key().value()
                )),
                lore
        );
    }

    @Action("label")
    private void handleSelect(@NotNull Player player, int slot, @NotNull ClickType type) {
        var value = switch (type) {
            case LEFT_CLICK -> {
                boolean found = false;
                for (var entry : this.values) {
                    if (found) {
                        yield entry.getFirst();
                    } else if (Objects.equals(entry.getFirst(), this.value)) {
                        found = true;
                    }
                }
                yield found ? this.values.getFirst().getFirst() : null;
            }
            case SHIFT_LEFT_CLICK -> this.defaultValue;
            default -> null;
        };
        if (Objects.equals(value, this.value)) return;

        this.value = value;
        this.performSignal(SIGNAL, this.component, this.value);
        this.updateLore();
    }


}
