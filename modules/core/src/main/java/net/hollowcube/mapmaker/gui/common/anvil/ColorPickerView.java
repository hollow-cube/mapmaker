package net.hollowcube.mapmaker.gui.common.anvil;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.RGBLike;
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

    private @Outlet("title") Text title;
    private @Outlet("input") Label input;
    private @Outlet("preview") Label preview;
    private @Outlet("output") Label output;

    private final TextInputBuilder<RGBLike, ColorPickerView> settings;

    private TextColor color;

    private ColorPickerView(
            @NotNull Context context,
            @NotNull TextInputBuilder<RGBLike, ColorPickerView> settings,
            @Nullable RGBLike input
    ) {
        super(context);
        this.settings = settings;

        this.color = Objects.requireNonNullElse(OpUtils.map(input, TextColor::color), NamedTextColor.BLACK);

        this.input.setItemSprite(this.input.getItemDirect().with(ItemComponent.HIDE_TOOLTIP));
        this.preview.setItemSprite(
                ItemStack.builder(Material.LEATHER_HORSE_ARMOR)
                        .set(ItemComponent.DYED_COLOR, new DyedItemColor(this.color, true))
                        .set(ItemComponent.HIDE_TOOLTIP)
                        .customModelData(COLOR_PREVIEW.cmd())
                        .build()
        );
        this.output.setItemSprite(this.output.getItemDirect().with(ItemComponent.HIDE_TOOLTIP));

        this.title.setText("Enter a color code:");
        this.input.setArgs(Component.text(this.color.asHexString()));
    }

    public static TextInputBuilder<RGBLike, ColorPickerView> builder() {
        return new TextInputBuilder<>(ColorPickerView::new);
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        var newColor = Objects.requireNonNullElse(TextColor.fromCSSHexString(input), NamedTextColor.BLACK);
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
}
