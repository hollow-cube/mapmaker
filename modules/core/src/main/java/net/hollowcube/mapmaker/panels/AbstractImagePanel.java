package net.hollowcube.mapmaker.panels;

import net.hollowcube.mapmaker.gui.common.ExtraPanels;
import net.hollowcube.mapmaker.util.ImageBuffer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.component.TooltipDisplay;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractImagePanel extends Panel {

    private static final int MAP_ID = 0;

    private final ImageBuffer buffer = new ImageBuffer();

    public AbstractImagePanel(@NotNull String infoKey) {
        super(InventoryType.CARTOGRAPHY, 9, 5);

        background("generic2/image/container", -20, -37);

        add(0, 0, new ImageElement(ExtraPanels.backOrClose()));
        add(1, 0, ExtraPanels.info(infoKey));
        add(2, 0, new Button("gui.generic.empty", 1, 1)
            .disableTooltip()
            .background("generic2/btn/success/1_1")
            .sprite("generic2/btn/common/confirm", 3, 3)
            .onLeftClick(this::onSubmit));
    }

    protected void onSubmit() {
        if (this.host != null) this.host.popView();
    }

    protected void updateImage(Consumer<ImageBuffer> updater) {
        updater.accept(this.buffer);
        this.host.player().sendPacket(this.buffer.preparePacket(MAP_ID));
    }

    private static class ImageElement extends Panel {

        public ImageElement(Element parent) {
            super(parent.slotWidth, parent.slotHeight);
            this.add(0, 0, parent);
        }

        @Override
        public void build(@NotNull MenuBuilder builder) {
            super.build(builder);
            builder.editSlots(0, 0, this.slotWidth, this.slotHeight, DataComponents.MAP_ID, MAP_ID);
            builder.editSlots(
                0, 0,
                this.slotWidth, this.slotHeight,
                DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, Set.of())
            );
        }
    }
}
