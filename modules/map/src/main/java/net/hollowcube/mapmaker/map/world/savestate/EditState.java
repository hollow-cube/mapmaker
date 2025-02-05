package net.hollowcube.mapmaker.map.world.savestate;

import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.hollowcube.common.util.dfu.ExtraCodecs;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.util.LegacyCodecs;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Map;
import java.util.Optional;

public final class EditState {
    @TestOnly
    static final Codec<Map<Integer, ItemStack>> INVENTORY_CODEC = Codec.withAlternative(
            Codec.unboundedMap(ExtraCodecs.INT_STRING, ExtraCodecs.ITEM_STACK),
            LegacyCodecs.ITEM_STACK_MAP_AS_BASE64
    );

    public static final Codec<EditState> CODEC = RecordCodecBuilder.create(i -> i.group(
            ExtraCodecs.POS.optionalFieldOf("pos").forGetter(EditState::pos),
            Codec.BOOL.optionalFieldOf("isFlying", false).forGetter(EditState::isFlying),
            INVENTORY_CODEC.optionalFieldOf("inventory", Map.of()).forGetter(EditState::inventory),
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

        @Override
        public @NotNull MCDataType dataType() {
            return HCTypeRegistry.EDIT_STATE;
        }
    };

    private Optional<Pos> pos;
    private boolean isFlying;
    private Map<Integer, ItemStack> inventory;
    private int selectedSlot;

    public EditState() {
        this(Optional.empty(), false, Map.of(), 0);
    }

    public EditState(Optional<Pos> pos, boolean isFlying, Map<Integer, ItemStack> inventory, int selectedSlot) {
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

    public @NotNull Map<Integer, ItemStack> inventory() {
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
        this.inventory = inventory;
    }

    public void setSelectedSlot(int selectedSlot) {
        this.selectedSlot = selectedSlot;
    }
}
