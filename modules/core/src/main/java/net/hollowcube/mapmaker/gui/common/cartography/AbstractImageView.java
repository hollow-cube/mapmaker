package net.hollowcube.mapmaker.gui.common.cartography;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.minestom.server.item.ItemComponent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class AbstractImageView extends View {

    private static final int MAP_ID = 0;

    private @Outlet("image") Label image;

    private final ImageBuffer buffer;

    protected AbstractImageView(@NotNull Context context) {
        super(context);

        this.buffer = new ImageBuffer();

        this.image.setItemSprite(this.image.getItemDirect().with(ItemComponent.MAP_ID, MAP_ID));
    }

    protected void updateImage(Consumer<ImageBuffer> updater) {
        updater.accept(this.buffer);
        this.player().sendPacket(this.buffer.preparePacket(MAP_ID));
    }

    @Action("image")
    public void goBack() {
        this.popView();
    }
}
