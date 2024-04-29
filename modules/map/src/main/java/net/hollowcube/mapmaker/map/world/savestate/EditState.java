package net.hollowcube.mapmaker.map.world.savestate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.util.EvenMoreCodecs;
import net.hollowcube.mapmaker.util.dfu.ExtraCodecs;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public final class EditState {
    public static final Codec<EditState> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.POS.optionalFieldOf("pos").forGetter(EditState::pos),
            Codec.BOOL.optionalFieldOf("isFlying", false).forGetter(EditState::isFlying),
            EvenMoreCodecs.ITEM_STACK_MAP_AS_BASE64.optionalFieldOf("inventory").forGetter(EditState::inventory),
            Codec.INT.optionalFieldOf("selectedSlot", 0).forGetter(EditState::selectedSlot)
    ).apply(i, EditState::new));

    public static final SaveStateType.Serializer<EditState> SERIALIZER = new SaveStateType.Serializer<>() {
        @Override
        public @NotNull String name() {
            return "editState";
        }

        @Override
        public @NotNull Codec<EditState> codec() {
            return CODEC;
        }
    };

    private Optional<Pos> pos;
    private boolean isFlying;
    private Optional<Map<Integer, ItemStack>> inventory;
    private int selectedSlot;

    public EditState() {
        this(Optional.empty(), false, Optional.empty(), 0);
    }

    public EditState(Optional<Pos> pos, boolean isFlying, Optional<Map<Integer, ItemStack>> inventory, int selectedSlot) {
        this.pos = pos;
        this.isFlying = isFlying;
        this.inventory = inventory;
        this.selectedSlot = selectedSlot;
    }

    public @NotNull Optional<Pos> pos() {
        return pos;
    }

    public boolean isFlying() {
        return isFlying;
    }

    public @NotNull Optional<Map<Integer, ItemStack>> inventory() {
        return inventory;
    }

    public int selectedSlot() {
        return selectedSlot;
    }

    public void setPos(@Nullable Pos pos) {
        this.pos = Optional.ofNullable(pos);
    }

    public void setFlying(boolean flying) {
        isFlying = flying;
    }

    public void setInventory(@NotNull Map<Integer, ItemStack> inventory) {
        this.inventory = Optional.of(inventory);
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }
}
