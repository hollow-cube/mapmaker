package net.hollowcube.mapmaker.map.gui.displayentity;

import com.mojang.datafixers.util.Pair;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ActionGroup;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.map.gui.displayentity.object.ComponentEntry;
import net.hollowcube.mapmaker.map.gui.displayentity.object.SetDisplayObject;
import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponent;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class EditItemDisplayView extends AbstractEditDisplayView<DisplayEntity.Item, ItemDisplayMeta> {

    private static final Predicate<Material> FILTER = material -> !material.isBlock() || !material.block().isAir();
    private static final Map<DataComponent<?>, List<Pair<?, Component>>> DISPLAY_COMPONENTS = Map.of(
            ItemComponent.ENCHANTMENT_GLINT_OVERRIDE, List.of(
                    Pair.of(null, Component.text("Default")),
                    Pair.of(Boolean.TRUE, Component.text("True")),
                    Pair.of(Boolean.FALSE, Component.text("False"))
            )
    );

    private @Outlet("option_context") Switch contextOption;

    private @Outlet("item") Label item;
    private @Outlet("components") Pagination components;

    public EditItemDisplayView(@NotNull Context context, DisplayEntity.Item display) {
        super(context, display);

        this.updateState();
    }

    @Override
    protected void updateState() {
        super.updateState();
        this.contextOption.setOption(this.meta().getDisplayContext().ordinal());
        this.item.setArgs(LanguageProviderV2.getVanillaTranslation(this.meta().getItemStack().material()));
    }

    // Options

    @ActionGroup("option_context_.*")
    public void onCycleContext() {
        var values = ItemDisplayMeta.DisplayContext.values();
        this.meta().setDisplayContext(values[(this.meta().getDisplayContext().ordinal() + 1) % values.length]);
        this.updateState();
    }

    // Display

    @Action("item")
    private void openItemChooser() {
        this.pushView(context -> new SetDisplayObject(context, FILTER));
    }

    @Action("components")
    private void fillComponentsList(@NotNull Pagination.PageRequest<ComponentEntry> request) {
        ItemStack stack = this.meta().getItemStack();
        ItemStack defaultStack = ItemStack.of(stack.material());

        List<ComponentEntry> result = new ArrayList<>();
        for (var entry : DISPLAY_COMPONENTS.entrySet()) {
            result.add(new ComponentEntry(
                    request.context(),
                    entry.getKey(),
                    entry.getValue(),
                    defaultStack.get(entry.getKey()),
                    stack.get(entry.getKey())
            ));
        }
        request.respond(result, false);
    }

    @Signal(ComponentEntry.SIGNAL)
    private void onSetComponent(@NotNull DataComponent<Object> component, @NotNull Object value) {
        this.meta().setItemStack(this.meta().getItemStack().with(component, value));
    }

    @Signal(SetDisplayObject.SIGNAL)
    private void onSetItem(Material material) {
        this.meta().setItemStack(ItemStack.of(material));
        this.updateState();
        this.components.reset();
    }

}
