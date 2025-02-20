package net.hollowcube.mapmaker.gui.common.anvil;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.color.AlphaColor;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.DyedItemColor;
import net.minestom.server.network.packet.server.play.SetSlotPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ColorPickerView extends View {

    private static final BadSprite COLOR_PREVIEW = BadSprite.require("anvil/color_preview");
    private static final AlphaColor BLACK = new AlphaColor(255, 0, 0, 0);

    private @Outlet("title") Text title;
    private @Outlet("input") Label input;
    private @Outlet("preview") Label preview;
    private @Outlet("output") Label output;

    private final TextInputBuilder<AlphaColor, ColorPickerView> settings;

    private AlphaColor color;

    private ColorPickerView(
            @NotNull Context context,
            @NotNull TextInputBuilder<AlphaColor, ColorPickerView> settings,
            @Nullable AlphaColor input
    ) {
        super(context);
        this.settings = settings;

        this.color = Objects.requireNonNullElse(input, BLACK);

        this.input.setItemSprite(this.input.getItemDirect().with(ItemComponent.HIDE_TOOLTIP));
        this.preview.setItemSprite(
                ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
                        .set(ItemComponent.DYED_COLOR, new DyedItemColor(this.color, true))
                        .set(ItemComponent.HIDE_TOOLTIP)
                        .customModelData(COLOR_PREVIEW.cmd())
                        .build()
        );
        this.output.setItemSprite(this.output.getItemDirect().with(ItemComponent.HIDE_TOOLTIP));

        this.title.setText(this.settings.title);
        String hex = this.color.alpha() == 0 ? String.format("#%02x%02x%02x", this.color.red(), this.color.green(), this.color.blue()) :
                String.format("#%02x%02x%02x%02x", this.color.alpha(), this.color.red(), this.color.green(), this.color.blue());
        this.input.setArgs(Component.text(hex));
    }

    public static TextInputBuilder<AlphaColor, ColorPickerView> builder() {
        return new TextInputBuilder<>(ColorPickerView::new);
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        var newColor = Objects.requireNonNullElse(parseColor(input), BLACK);
        if (newColor.equals(this.color)) return;
        this.color = newColor;

        var player = player();
        var inventory = player.getOpenInventory();
        if (inventory == null) return;

        player.sendPacket(new SetSlotPacket(
                inventory.getWindowId(),
                0,
                (short) 1,
                this.preview
                        .getItemDirect()
                        .with(ItemComponent.DYED_COLOR, new DyedItemColor(this.color, true))
        ));
    }

    @Action("input")
    public void handleBackButton() {
        popView();
    }

    @Action("preview")
    public void handlePreview() {
        this.preview.setItemSprite(this.preview
                .getItemDirect()
                .with(ItemComponent.DYED_COLOR, new DyedItemColor(this.color, true))
        );
    }

    @Action("output")
    public void handleAccept() {
        if (this.settings.callback != null) {
            this.settings.callback.accept(this.color);
            this.popView();
        } else {
            popView(this.settings.signal, this.color);
        }
    }

    private @Nullable AlphaColor parseColor(@NotNull String input) {
        if (input.startsWith("#")) input = input.substring(1);
        else if (input.startsWith("0x")) input = input.substring(2);

        try {
            if (input.length() == 3 || input.length() == 4) {
                var one = Integer.parseInt(input.substring(0, 1), 16) * 17;
                var two = Integer.parseInt(input.substring(1, 2), 16) * 17;
                var three = Integer.parseInt(input.substring(2, 3), 16) * 17;
                if (input.length() == 3) {
                    return new AlphaColor(255, one, two, three);
                } else{
                    var four = Integer.parseInt(input.substring(3, 4), 16) * 17;
                    return new AlphaColor(one, two, three, four);
                }
            } else if (input.length() == 6 || input.length() == 8) {
                var one = Integer.parseInt(input.substring(0, 2), 16);
                var two = Integer.parseInt(input.substring(2, 4), 16);
                var three = Integer.parseInt(input.substring(4, 6), 16);
                if (input.length() == 6) {
                    return new AlphaColor(255, one, two, three);
                } else {
                    var four = Integer.parseInt(input.substring(6, 8), 16);
                    return new AlphaColor(one, two, three, four);
                }
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }
}
