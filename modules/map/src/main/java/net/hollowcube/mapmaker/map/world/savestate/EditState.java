package net.hollowcube.mapmaker.map.world.savestate;

import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.datafix.DataType;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.util.LegacyCodecs;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class EditState {
    @TestOnly
    static final Codec<Map<Integer, ItemStack>> INVENTORY_CODEC = ExtraCodecs.INT_STRING.mapValue(ItemStack.CODEC)
            .orElse(LegacyCodecs.ITEM_STACK_MAP_AS_BASE64);

    public static final StructCodec<EditState> CODEC = StructCodec.struct(
            "pos", ExtraCodecs.POS.optional(), EditState::pos,
            "isFlying", Codec.BOOLEAN.optional(false), EditState::isFlying,
            "inventory", INVENTORY_CODEC.optional(Map.of()), EditState::inventory,
            "selectedSlot", Codec.INT.optional(0), EditState::selectedSlot,
            EditState::new);

    public static final SaveStateType.Serializer<EditState> SERIALIZER = new SaveStateType.Serializer<>() {
        @Override
        public @NotNull String name() {
            return "editState";
        }

        @Override
        public @NotNull Codec<EditState> codec() {
            return CODEC;
        }

        @Override
        public @NotNull DataType dataType() {
            return HCDataTypes.EDIT_STATE;
        }
    };

    private Pos pos;
    private boolean isFlying;
    private Map<Integer, ItemStack> inventory;
    private int selectedSlot;

    public EditState() {
        this(null, false, Map.of(), 0);
    }

    public EditState(@Nullable Pos pos, boolean isFlying, Map<Integer, ItemStack> inventory, int selectedSlot) {
        this.pos = pos;
        this.isFlying = isFlying;
        this.inventory = inventory;
        this.selectedSlot = selectedSlot;
    }

    public @Nullable Pos pos() {
        return pos;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public @NotNull Map<Integer, ItemStack> inventory() {
        return inventory;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public void setPos(@Nullable Pos pos) {
        this.pos = pos;
    }

    public void setFlying(boolean flying) {
        isFlying = flying;
    }

    public void setInventory(@NotNull Map<Integer, ItemStack> inventory) {
        this.inventory = inventory;
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }
}
