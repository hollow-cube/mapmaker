package net.hollowcube.mapmaker.map.util.datafix;

import ca.spottedleaf.dataconverter.util.ExternalDataProvider;
import com.google.auto.service.AutoService;
import com.mojang.serialization.DynamicOps;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.util.dfu.NbtOps;
import net.kyori.adventure.nbt.BinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@AutoService(ExternalDataProvider.class)
public class HCDataProvider implements ExternalDataProvider {

    @Override
    public int dataVersion() {
        return MapWorld.DATA_VERSION;
    }

    @Override
    public @NotNull List<Integer> extraConverterVersions() {
        return List.of(
                HCVersions.V1_20_4_HC1,
                HCVersions.V1_20_5_HC1
        );
    }

    @Override
    public @NotNull Class<?> extraVersionsClass() {
        return HCVersions.class;
    }

    @Override
    public @NotNull DynamicOps<BinaryTag> nbtOps() {
        return NbtOps.INSTANCE;
    }
}
