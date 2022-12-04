package net.hollowcube.canvas;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public sealed class RootSection extends ParentSection permits RouterSection {
    private int width, height;
    private Inventory inventory;
    private Section section = null;

    public RootSection(@NotNull Section section) {
        // We override these methods, so their value is just a marker in case it crops up somewhere it shouldnt.
        super(Integer.MAX_VALUE, Integer.MAX_VALUE);
        replaceInventory(section);
    }

    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @Override
    public <C extends Section> @UnknownNullability C find(Class<C> componentType) {
        if (componentType.isAssignableFrom(getClass())) return componentType.cast(this);
        return null;
    }

    public void setTitle(@NotNull net.kyori.adventure.text.Component title) {
        inventory.setTitle(title);
    }

    // Item manipulation

    @Override
    void updateItem(int index, @NotNull ItemStack itemStack) {
        inventory.setItemStack(index, itemStack);
        //todo does not need to call item events, and should wait/batch updates on tick perhaps
    }

    // Click handling

    private boolean tryHandleClick(int index, @NotNull Player player, @NotNull ClickType clickType) {
        if (index == -999) {
            // Clicked outside inventory, todo handle this properly
            return false;
        }

        // Ensure the index is inside the component
        int x = index % width();
        int y = index / width();
        if (x >= section.width() || y >= section.height()) {
            return false;
        }

        // Convert the index to the component's coordinate system and call the component's click handler
        index = x + y * section.width();
        return section.handleClick(index, player, clickType);
    }

    // Internal details


    @Override
    int _width() {
        return width;
    }

    @Override
    int _height() {
        return height;
    }

    protected void replaceInventory(@NotNull Section newSection) {
        // Unmount old component if relevant
        Inventory oldInv = this.inventory;
        if (this.section != null) {
            this.section.removeParent();
            this.inventory.getInventoryConditions().clear(); // Do not want ghost clicks or anything
        }

        this.inventory = new Inventory(updateSize(newSection), "Chest");
        this.section = newSection;
        this.section.setParent(this, 0);

        inventory.addInventoryCondition((player, slot, clickType, result) -> {
            var allow = tryHandleClick(slot, player, clickType);
            result.setCancel(!allow);
        });

        if (oldInv != null) {
            // Migrate the viewers to the new inventory
            for (Player player : oldInv.getViewers()) {
                player.openInventory(inventory);
            }
        }
    }

    private @NotNull InventoryType updateSize(@NotNull Section section) {
        Check.argCondition(section.width() > 9, "component width must be <= 9, was {}", section.width());
        Check.argCondition(section.height() > 6, "component height must be <= 6, was {}", section.height());
        width = section.width();
        height = section.height();
        return switch (section.height()) {
            case 1 -> InventoryType.CHEST_1_ROW;
            case 2 -> InventoryType.CHEST_2_ROW;
            case 3 -> InventoryType.CHEST_3_ROW;
            case 4 -> InventoryType.CHEST_4_ROW;
            case 5 -> InventoryType.CHEST_5_ROW;
            case 6 -> InventoryType.CHEST_6_ROW;
            default -> throw new IllegalStateException("Unreachabe");
        };
    }
}
